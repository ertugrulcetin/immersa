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
      (cond-> {:db (assoc-in db [:editor :selected-mesh :scaling] scaling)}

        (not (str/blank? value))
        (assoc :scene {:type :update-selected-mesh-text-depth-or-size
                       :data {:update type
                              :value (parse-double value)}})))))

(reg-event-fx
  ::add-text-mesh
  (fn []
    {:scene {:type :add-text-mesh}}))

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
                      :value value}}})))
