(ns immersa.ui.editor.events
  (:require
    [applied-science.js-interop :as j]
    [clojure.string :as str]
    [immersa.common.communication :refer [fire]]
    [immersa.common.firebase :as firebase]
    [immersa.ui.crisp-chat :as crisp-chat]
    [immersa.ui.events]
    [medley.core :refer [dissoc-in]]
    [re-frame.core :refer [reg-event-db reg-event-fx reg-fx]]))

(def start-scene nil)

(reg-event-db
  ::set-canvas-wrapper-dimensions
  (fn [db [_ width height]]
    (-> db
        (assoc-in [:editor :canvas-wrapper :width] width)
        (assoc-in [:editor :canvas-wrapper :height] height))))

(reg-event-db
  ::remove-selected-mesh
  (fn [db]
    (dissoc-in db [:editor :selected-mesh])))

(reg-event-fx
  ::clear-selected-mesh
  (fn []
    {:scene {:type :clear-selected-mesh}}))

(reg-event-fx
  ::update-selected-mesh
  (fn [{:keys [db]} [_ type index value]]
    (let [selected-mesh (-> db :editor :selected-mesh)
          selected-mesh (assoc-in selected-mesh [type index] value)
          updated-attr (type selected-mesh)]
      (cond-> {:db (assoc-in db [:editor :selected-mesh] selected-mesh)}

        (not (str/blank? value))
        (assoc :scene {:type :update-selected-mesh
                       :data {:update type
                              :value (mapv parse-double updated-attr)}})))))

(reg-event-db
  ::set-camera
  (fn [db [_ {:keys [position rotation]}]]
    (-> db
        (assoc-in [:editor :camera :position] position)
        (assoc-in [:editor :camera :rotation] rotation))))

(reg-event-fx
  ::update-camera
  (fn [{:keys [db]} [_ type index value]]
    (let [camera (-> db :editor :camera)
          camera (assoc-in camera [type index] value)
          updated-attr (type camera)]
      (cond-> {:db (assoc-in db [:editor :camera] camera)}

        (not (str/blank? value))
        (assoc :scene {:type :update-camera
                       :data {:update type
                              :value (mapv parse-double updated-attr)}})))))

(reg-event-fx
  ::update-scene-background-color
  (fn [_ [_ rgb]]
    {:scene {:type :update-background-color
             :data {:value rgb}}
     :analytics {:event "scene-background-color-update"}}))

(reg-event-fx
  ::update-scene-background-brightness
  (fn [_ [_ value]]
    (let [value (/ (first value) 100)]
      {:scene {:type :update-background-brightness
               :data {:value value}}
       :analytics {:event "scene-background-brightness-update"}})))

#_(reg-event-db
    ::update-scene-background-color-success
    (fn [db [_ rgb]]
      (assoc-in db [:editor :scene :background-color] rgb)))

(reg-event-fx
  ::resize-scene
  (fn []
    {:scene {:type :resize}}))

(reg-fx
  :scene
  (fn [data]
    (fire :get-ui-update data)))

(reg-event-fx
  ::update-selected-mesh-main-color
  (fn [{:keys [db]} [_ rgb]]
    {:db (assoc-in db [:editor :selected-mesh :color] rgb)
     :scene {:type :update-selected-mesh-color
             :data {:value rgb}}}))

(reg-event-db
  ::set-selected-text3D-data
  (fn [db [_ data]]
    (assoc-in db [:editor :selected-mesh] data)))

(reg-event-db
  ::set-selected-glb-data
  (fn [db [_ data]]
    (assoc-in db [:editor :selected-mesh] data)))

(reg-event-db
  ::set-selected-image-data
  (fn [db [_ data]]
    (assoc-in db [:editor :selected-mesh] data)))

(reg-event-db
  ::update-selected-mesh-rotation-axis
  (fn [db [_ axis value]]
    (let [index (case axis
                  :x 0
                  :y 1
                  :z 2)]
      (assoc-in db [:editor :selected-mesh :rotation index] value))))

(reg-event-fx
  ::update-selected-mesh-slider-value
  (fn [{:keys [db]} [_ type value]]
    (let [value (/ (first value) 100)
          selected-mesh (-> db :editor :selected-mesh (assoc type value))]
      {:db (assoc-in db [:editor :selected-mesh] selected-mesh)
       :scene {:type :update-selected-mesh-slider-value
               :data {:update type
                      :value value}}})))

(reg-event-fx
  ::update-selected-mesh-text-content
  (fn [{:keys [db]} [_ value]]
    (cond-> {:db (assoc-in db [:editor :selected-mesh :text] value)}
      (not (str/blank? value)) (assoc :scene {:type :update-selected-mesh-text-content
                                              :data {:value value}}))))

(reg-event-fx
  ::update-selected-mesh-text-depth-or-size
  (fn [{:keys [db]} [_ type value]]
    (let [selected-mesh (-> db :editor :selected-mesh)
          [x y z] (:scaling selected-mesh)
          scaling (case type
                    :size [value value z]
                    :depth [x y value])]
      (cond-> {:db (assoc-in db [:editor :selected-mesh :scaling] scaling)
               :analytics {:event "text-depth-or-size-update"
                           :data {:update type
                                  :value value}}}

        (not (str/blank? value))
        (assoc :scene {:type :update-selected-mesh-text-depth-or-size
                       :data {:update type
                              :value (parse-double value)}})))))

(reg-event-fx
  ::add-text-mesh
  (fn []
    {:scene {:type :add-text-mesh}
     :analytics {:event "text-add"}}))

(reg-event-fx
  ::add-image
  (fn [_ [_ url]]
    {:scene {:type :add-image
             :data {:value url}}
     :analytics {:event "image-add"}}))

(reg-event-fx
  ::add-model
  (fn [_ [_ url]]
    {:scene {:type :add-model
             :data {:value url}}
     :analytics {:event "model-add"}}))

(reg-event-fx
  ::go-to-slide
  (fn [_ [_ index]]
    {:scene {:type :go-to-slide
             :data {:index index}}}))

(reg-event-db
  ::set-current-slide-index
  (fn [db [_ current-index]]
    (assoc-in db [:editor :slides :current-index] current-index)))

(reg-event-fx
  ::add-slide
  (fn []
    {:scene {:type :add-slide}}))

(reg-event-fx
  ::re-order-slides
  (fn [_ [_ old-index new-index]]
    {:scene {:type :re-order-slides
             :data {:value [old-index new-index]}}
     :analytics {:event "slide-re-order"}}))

(reg-event-db
  ::sync-slides-info
  (fn [db [_ {:keys [current-index slides]}]]
    (-> db
        (assoc-in [:editor :slides :current-index] current-index)
        (assoc-in [:editor :slides :all] slides))))

(reg-event-db
  ::sync-thumbnails
  (fn [db [_ thumbnails]]
    (let [slide-ids (->> db :editor :slides :all (map :id))
          thumbnails (-> db :editor :slides :thumbnails (merge thumbnails) (select-keys slide-ids))]
      (assoc-in db [:editor :slides :thumbnails] thumbnails))))

(reg-event-fx
  ::update-gizmo-visibility
  (fn [{:keys [db]} [_ type]]
    (let [gizmo (-> db :editor :gizmo type)
          value (not gizmo)]
      {:db (assoc-in db [:editor :gizmo type] value)
       :scene {:type :update-gizmo-visibility
               :data {:update type
                      :value value}}
       :analytics {:event "gizmo-update"
                   :data {:gizmo type
                          :value value}}})))

(reg-event-db
  ::notify-gizmo-state
  (fn [db [_ type enabled?]]
    (assoc-in db [:editor :gizmo type] enabled?)))

(reg-event-db
  ::notify-camera-lock-state
  (fn [db [_ locked?]]
    (assoc-in db [:editor :camera :locked?] locked?)))

(reg-event-db
  ::notify-ground-state
  (fn [db [_ enabled?]]
    (assoc-in db [:editor :ground :enabled?] enabled?)))

(reg-event-fx
  ::update-selected-mesh-face-to-screen?
  (fn [{:keys [db]} _]
    (let [face-to-screen? (-> db :editor :selected-mesh :face-to-screen? not)]
      {:db (assoc-in db [:editor :selected-mesh :face-to-screen?] face-to-screen?)
       :scene {:type :update-selected-mesh-face-to-screen?
               :data {:value face-to-screen?}}})))

(reg-event-fx
  ::update-selected-image-mesh-transparent?
  (fn [{:keys [db]} _]
    (let [transparent? (-> db :editor :selected-mesh :transparent? not)]
      {:db (assoc-in db [:editor :selected-mesh :transparent?] transparent?)
       :scene {:type :update-selected-image-mesh-transparent?
               :data {:value transparent?}}})))

(reg-event-fx
  ::toggle-camera-lock
  (fn [{:keys [db]} _]
    (let [locked? (-> db :editor :camera :locked? not)]
      {:scene {:type :toggle-camera-lock
               :data {:value locked?}}
       :analytics {:event "camera-lock-update"
                   :data {:locked? locked?}}})))

(reg-event-fx
  ::toggle-ground-enabled
  (fn [{:keys [db]} _]
    (let [enabled? (-> db :editor :ground :enabled? not)]
      {:scene {:type :toggle-ground-enabled
               :data {:value enabled?}}
       :analytics {:event "ground-update"
                   :data {:enabled? enabled?}}})))

(reg-event-db
  ::set-context-menu-position
  (fn [db [_ position]]
    (assoc-in db [:editor :context-menu :position] position)))

(reg-event-db
  ::clear-context-menu-position
  (fn [db]
    (dissoc-in db [:editor :context-menu :position])))

(reg-event-fx
  ::update-thumbnail
  (fn []
    {:scene {:type :create-slide-thumbnail}}))

(reg-event-fx
  ::open-crisp-chat
  (fn []
    {:fx [[::open-crisp-chat]]
     :analytics {:event "feedback-open"}}))

(reg-fx
  ::open-crisp-chat
  (fn []
    (crisp-chat/toggle)))

(reg-event-db
  ::init-user
  (fn [db [_ user]]
    (assoc db :user user)))

(reg-event-fx
  ::init-presentation
  (fn [{:keys [db]} [_ {:keys [id title slides thumbnails present-state] :as opts}]]
    {:db (-> db
             (assoc-in [:editor :slides :id] id)
             (assoc-in [:editor :slides :title] title)
             (assoc-in [:editor :slides :all] slides)
             (assoc-in [:editor :slides :thumbnails] thumbnails)
             (assoc-in [:editor :slides :present-state] present-state))
     ::start-scene opts
     ::set-title title}))

(reg-fx
  ::start-scene
  (fn [{:keys [slides thumbnails present-state]}]
    (start-scene (js/document.getElementById "renderCanvas")
                 {:mode :editor
                  :present-state present-state
                  :slides slides
                  :thumbnails thumbnails})))

(reg-fx
  ::set-title
  (fn [title]
    (j/assoc! js/document :title (str title " - Immersa"))))

(reg-event-db
  ::add-thumbnail
  (fn [db [_ slide-id url]]
    (assoc-in db [:editor :slides :thumbnails slide-id] url)))

(reg-event-db
  ::add-uploaded-image
  (fn [db [_ image]]
    (update-in db [:user :images] (fnil conj []) image)))

(reg-event-db
  ::add-uploaded-model
  (fn [db [_ model]]
    (update-in db [:user :models] (fnil conj []) model)))

(reg-event-fx
  ::set-user-id
  (fn [{:keys [db]}]
    {:scene {:type :set-user-id
             :data {:id (-> db :user :id)}}}))

(reg-event-fx
  ::set-presentation-id
  (fn [{:keys [db]}]
    {:scene {:type :set-presentation-id
             :data {:id (-> db :editor :slides :id)}}}))

(reg-event-fx
  ::scene-ready
  (fn [{:keys [db]}]
    {:db (assoc-in db [:editor :scene-ready?] true)
     :dispatch-n [[::set-user-id]
                  [::set-presentation-id]]}))

(reg-event-fx
  ::attach-camera-controls
  (fn []
    {:scene {:type :attach-camera-controls}}))

(reg-event-fx
  ::detach-camera-controls
  (fn []
    {:scene {:type :detach-camera-controls}}))

(reg-event-fx
  ::update-slide-info
  (fn []
    {:scene {:type :update-slide-info}}))

(reg-event-fx
  ::add-listeners-for-present-mode
  (fn []
    {:scene {:type :add-listeners-for-present-mode}}))

(reg-event-fx
  ::remove-listeners-for-present-mode
  (fn []
    {:scene {:type :remove-listeners-for-present-mode}}))

(reg-event-db
  ::notify-undo-redo-state
  (fn [db [_ state]]
    (update db :editor merge state)))

(reg-event-db
  ::save-started-index
  (fn [db]
    (assoc-in db [:editor :present :started-index] (-> db :editor :slides :current-index))))

(reg-event-fx
  ::create-go-to-slide-action
  (fn [{:keys [db]}]
    {:scene {:type :create-go-to-slide-action
             :data {:from (-> db :editor :present :started-index)
                    :to (-> db :editor :slides :current-index)}}}))

(reg-event-fx
  ::update-presentation-title
  (fn [{:keys [db]} [_ title]]
    (let [id (-> db :editor :slides :id)
          user-id (-> db :user :id)]
      {:db (assoc-in db [:editor :slides :title] title)
       :update-presentation-title {:user-id user-id
                                   :presentation-id id
                                   :title title}
       ::set-title title
       :analytics {:event "title-update"}})))

(reg-fx
  :update-presentation-title
  (fn [opts]
    (firebase/update-presentation-title opts)))
