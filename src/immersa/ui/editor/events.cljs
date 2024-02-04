(ns immersa.ui.editor.events
  (:require
    [clojure.string :as str]
    [immersa.common.communication :refer [fire]]
    [medley.core :refer [dissoc-in]]
    [re-frame.core :refer [reg-event-db reg-event-fx reg-fx]]))

(reg-event-db
  ::set-canvas-wrapper-dimensions
  (fn [db [_ width height]]
    (-> db
        (assoc-in [:editor :canvas-wrapper :width] width)
        (assoc-in [:editor :canvas-wrapper :height] height))))

(reg-event-db
  ::set-selected-mesh
  (fn [db [_ data]]
    (assoc-in db [:editor :selected-mesh] data)))

(reg-event-db
  ::clear-selected-mesh
  (fn [db]
    (dissoc-in db [:editor :selected-mesh])))

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
  (fn [{:keys [db]} [_ rgb]]
    {:db (assoc-in db [:editor :scene :background-color] rgb)
     :scene {:type :update-background-color
             :data {:value rgb}}}))

(reg-event-fx
  ::resize-scene
  (fn []
    {:scene {:type :resize}}))

(reg-fx
  :scene
  (fn [data]
    (fire :get-ui-update data)))

(reg-fx
  ::update-selected-mesh-color
  (fn [{:keys [db]}]
    {:db db}))
