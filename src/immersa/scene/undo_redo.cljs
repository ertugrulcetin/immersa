(ns immersa.scene.undo-redo
  (:require
    [applied-science.js-interop :as j]
    [clojure.string :as str]
    [com.rpl.specter :as sp]
    [immersa.common.utils :as common.utils]
    [immersa.scene.api.core :as api.core]
    [immersa.scene.api.mesh :as api.mesh]
    [immersa.scene.slide :as slide]
    [immersa.scene.ui-notifier :as ui.notifier]
    [immersa.scene.utils :as utils]
    [immersa.ui.editor.events :as events]
    [re-frame.core :refer [dispatch]]))

(defonce undo-stack #js [])
(defonce redo-stack #js [])

(defonce order (atom 0))
(def capacity 100)

(def blocked-fields #{"INPUT" "TEXTAREA"})

(def action->reverse-action
  {:create-text :dispose-text
   :update-position :revert-position
   :update-rotation :revert-rotation
   :update-scale :revert-scale
   :go-to-slide :back-to-slide
   :duplicate-slide :revert-duplicate-slide
   :delete-slide :revert-delete-slide
   :blank-slide :revert-blank-slide})

(defmulti execute :type)

(defmethod execute :create-text [{:keys [id params]}]
  (let [mesh (api.mesh/text id (slide/parse-slides params))]
    (slide/add-slide-data mesh params)))

(defmethod execute :dispose-text [{:keys [id]}]
  (slide/delete-slide-data id)
  (api.core/dispose id))

(defmethod execute :update-position [{:keys [id params]}]
  (let [to (:to params)
        mesh (api.core/get-object-by-name id)]
    (j/assoc! mesh :position (api.core/clone to))
    (some-> mesh (slide/update-slide-data :position (api.core/v3->v (j/get mesh :position))))
    (ui.notifier/notify-ui-selected-mesh mesh)))

(defmethod execute :revert-position [{:keys [id params]}]
  (let [from (:from params)
        mesh (api.core/get-object-by-name id)]
    (j/assoc! mesh :position (api.core/clone from))
    (some-> mesh (slide/update-slide-data :position (api.core/v3->v (j/get mesh :position))))
    (ui.notifier/notify-ui-selected-mesh mesh)))

(defmethod execute :update-rotation [{:keys [id params]}]
  (let [to (:to params)
        mesh (api.core/get-object-by-name id)]
    (j/assoc! mesh :rotation (api.core/clone to))
    (some-> mesh (slide/update-slide-data :rotation (api.core/v3->v (j/get mesh :rotation))))
    (ui.notifier/notify-ui-selected-mesh mesh)))

(defmethod execute :revert-rotation [{:keys [id params]}]
  (let [from (:from params)
        mesh (api.core/get-object-by-name id)]
    (j/assoc! mesh :rotation (api.core/clone from))
    (some-> mesh (slide/update-slide-data :rotation (api.core/v3->v (j/get mesh :rotation))))
    (ui.notifier/notify-ui-selected-mesh mesh)))

(defmethod execute :update-scale [{:keys [id params]}]
  (let [to (:to params)
        mesh (api.core/get-object-by-name id)]
    (j/assoc! mesh :scaling (api.core/clone to))
    (some-> mesh (slide/update-slide-data :scale (api.core/v3->v (j/get mesh :scaling))))
    (ui.notifier/notify-ui-selected-mesh mesh)))

(defmethod execute :revert-scale [{:keys [id params]}]
  (let [from (:from params)
        mesh (api.core/get-object-by-name id)]
    (j/assoc! mesh :scaling (api.core/clone from))
    (some-> mesh (slide/update-slide-data :scale (api.core/v3->v (j/get mesh :scaling))))
    (ui.notifier/notify-ui-selected-mesh mesh)))

;; TODO need to find a way for async actions like go-to-slide and back-to-slide
(defmethod execute :go-to-slide [{{:keys [to items]} :params}]
  (let [index (.indexOf items to)]
    (dispatch [::events/go-to-slide index])
    ;; to be able to force animations to be executed
    (js/setTimeout #(dispatch [::events/go-to-slide index]) 10)))

(defmethod execute :back-to-slide [{{:keys [from items]} :params}]
  (let [index (.indexOf items from)]
    (dispatch [::events/go-to-slide index])
    ;; to be able to force animations to be executed
    (js/setTimeout #(dispatch [::events/go-to-slide index]) 10)))

(defmethod execute :duplicate-slide [{{:keys [index slide]} :params}]
  (api.core/clear-selected-mesh)
  (swap! slide/all-slides #(utils/vec-insert % slide index))
  (reset! slide/current-slide-index index)
  (ui.notifier/sync-slides-info @slide/current-slide-index @slide/all-slides))

(defmethod execute :revert-duplicate-slide [{{:keys [index]} :params}]
  (api.core/clear-selected-mesh)
  (sp/setval [sp/ATOM index] sp/NONE slide/all-slides)
  (swap! slide/current-slide-index dec)
  (ui.notifier/sync-slides-info @slide/current-slide-index @slide/all-slides))

(defmethod execute :delete-slide [{{:keys [index]} :params}]
  (slide/delete-slide index))

(defmethod execute :revert-delete-slide [{{:keys [index slide selected-index-before]} :params}]
  (api.core/clear-selected-mesh)
  (reset! slide/current-slide-index selected-index-before)
  (swap! slide/all-slides #(utils/vec-insert % slide index))
  (ui.notifier/sync-slides-info @slide/current-slide-index @slide/all-slides))

(defmethod execute :blank-slide [{{:keys [index slide]} :params}]
  (api.core/clear-selected-mesh)
  (swap! slide/all-slides #(utils/vec-insert % slide index))
  (reset! slide/current-slide-index index)
  (slide/go-to-slide index)
  (ui.notifier/sync-slides-info @slide/current-slide-index @slide/all-slides))

(defmethod execute :revert-blank-slide [{{:keys [index]} :params}]
  (api.core/clear-selected-mesh)
  (sp/setval [sp/ATOM index] sp/NONE slide/all-slides)
  (swap! slide/current-slide-index dec)
  (slide/go-to-slide @slide/current-slide-index)
  (ui.notifier/sync-slides-info @slide/current-slide-index @slide/all-slides))

(defmethod execute :default [name]
  (println "default execute!"))

(defn- notify-ui []
  (ui.notifier/notify-undo-redo-state {:undo? (> (count undo-stack) 0)
                                       :redo? (> (count redo-stack) 0)}))

(defn create-action [action]
  (j/call undo-stack :push (assoc action :order (swap! order inc)))
  (when (> (j/get undo-stack :length) capacity)
    (j/call undo-stack :shift))
  (j/assoc! redo-stack :length 0)
  (notify-ui))

(defn undo []
  (let [action (j/call undo-stack :pop)]
    (when action
      (execute (update action :type action->reverse-action))
      (j/call redo-stack :push action))
    (notify-ui)
    undo-stack))

(defn redo []
  (let [action (j/call redo-stack :pop)]
    (when action
      (execute action)
      (j/call undo-stack :push action))
    (notify-ui)))

(defn init [present-state]
  (common.utils/register-event-listener
    js/window
    "keydown"
    (fn [e]
      (when (and (not @present-state)
                 (not (j/get e :repeat))
                 (not (blocked-fields (j/get-in js/document [:activeElement :tagName]))))
        (when (and (or (j/get e :metaKey)
                       (j/get e :ctrlKey))
                   (not (j/get e :shiftKey))
                   (= (some-> (j/get e :key) str/lower-case) "z"))
          (.preventDefault e)
          (undo))
        (when (or (and (or (j/get e :metaKey)
                           (j/get e :ctrlKey))
                       (= (some-> (j/get e :key) str/lower-case) "y"))
                  (and (or (j/get e :metaKey)
                           (j/get e :ctrlKey))
                       (j/get e :shiftKey)
                       (= (some-> (j/get e :key) str/lower-case) "z")))
          (.preventDefault e)
          (redo))))))

(comment

  (undo)
  (redo)

  undo-stack
  redo-stack

  (j/assoc! undo-stack :length 0)
  (j/assoc! redo-stack :length 0)
  (j/call undo-stack :push "a")
  (j/call undo-stack :pop)
  )
