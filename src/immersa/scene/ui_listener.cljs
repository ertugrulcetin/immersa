(ns immersa.scene.ui-listener
  (:require
    [applied-science.js-interop :as j]
    [com.rpl.specter :as sp]
    [goog.functions :as functions]
    [immersa.common.communication :refer [event-bus-pub]]
    [immersa.scene.api.camera :as api.camera]
    [immersa.scene.api.constant :as api.const]
    [immersa.scene.api.core :as api.core :refer [v3]]
    [immersa.scene.api.mesh :as api.mesh]
    [immersa.scene.slide :as slide]
    [immersa.ui.editor.events :as editor.events]
    [re-frame.core :refer [dispatch]])
  (:require-macros
    [immersa.common.macros :refer [go-loop-sub]]))

(defmulti handle-ui-update :type)

(defmethod handle-ui-update :update-selected-mesh [{{:keys [update value]} :data}]
  (when-let [mesh (j/get-in api.core/db [:gizmo :selected-mesh])]
    (case update
      :position (do
                  (slide/update-slide-data mesh :position value)
                  (j/assoc! mesh :position (api.core/v->v3 value)))
      :rotation (do
                  (slide/update-slide-data mesh :rotation (mapv api.core/to-rad value))
                  (j/assoc! mesh :rotation (api.core/v->v3 (mapv api.core/to-rad value))))
      :scaling (do
                 (slide/update-slide-data mesh :scale value)
                 (j/assoc! mesh :scaling (api.core/v->v3 value))))))

(let [lock-fn-id (atom nil)]
  (defmethod handle-ui-update :update-camera [{{:keys [update value]} :data}]
    (when-let [camera (api.camera/active-camera)]
      (j/assoc! api.core/db :lock-view-matrix-change? true)
      (case update
        :position (j/assoc! camera :position (api.core/v->v3 value))
        :rotation (j/assoc! camera :rotation (api.core/v->v3 (mapv api.core/to-rad value))))
      (some-> @lock-fn-id js/clearTimeout)
      (reset! lock-fn-id (js/setTimeout
                           (fn []
                             (reset! lock-fn-id nil)
                             (j/assoc! api.core/db :lock-view-matrix-change? false))
                           100)))))

(defmethod handle-ui-update :update-background-color [{{:keys [value]} :data}]
  (let [skybox-material (j/get-in api.core/db [:environment-helper :skybox :material])
        ground-material (j/get-in api.core/db [:environment-helper :ground :material])
        new-color (apply api.core/color-rgb value)]
    (j/assoc! skybox-material :primaryColor new-color)
    (j/assoc! ground-material :primaryColor new-color)
    (slide/update-slide-data :skybox [:background :color] value)
    (dispatch [::editor.events/sync-slides-info {:current-index @slide/current-slide-index
                                                 :slides @slide/all-slides}])))

(defmethod handle-ui-update :update-selected-mesh-slider-value [{{:keys [update value]} :data}]
  (when-let [mesh (j/get-in api.core/db [:gizmo :selected-mesh])]
    (case update
      :emissive-intensity (j/assoc-in! mesh [:material :emissiveIntensity] value)
      :opacity (j/assoc! mesh :visibility value)
      :alpha (j/assoc-in! mesh [:material :alpha] value)
      :metallic (j/assoc-in! mesh [:material :metallic] value)
      :roughness (j/assoc-in! mesh [:material :roughness] value))))

(defmethod handle-ui-update :update-selected-mesh-color [{{:keys [value]} :data}]
  (when-let [mesh (j/get-in api.core/db [:gizmo :selected-mesh])]
    (let [new-color (apply api.core/color-rgb value)]
      (j/assoc-in! mesh [:material :albedoColor] new-color)
      (j/assoc-in! mesh [:material :emissiveColor] new-color)
      (slide/update-slide-data mesh :color value))))

(defmethod handle-ui-update :update-selected-mesh-emissive-color [{{:keys [value]} :data}]
  (when-let [mesh (j/get-in api.core/db [:gizmo :selected-mesh])]
    (j/assoc-in! mesh [:material :emissiveColor] (apply api.core/color-rgb value))))

;; Info text pseudo code
;;
;; bb (j/get (j/call mesh :getBoundingInfo) :boundingBox)
;;              center (j/get bb :center)
;;              scale-factor 1.2
;;              width (* scale-factor (- (j/get-in bb [:maximum :x]) (j/get-in bb [:minimum :x])))
;;              height (* scale-factor (- (j/get-in bb [:maximum :y]) (j/get-in bb [:minimum :y])))
;;              plane (api.mesh/plane-rounded "p" {:radius 0.1
;;                                       :width width
;;                                       :height height
;;                                       :position center})

(def update-text-mesh
  (functions/debounce
    (fn [data]
      (when-let [mesh (j/get-in api.core/db [:gizmo :selected-mesh])]
        (let [material (api.core/clone (j/get mesh :material))
              position (api.core/clone (j/get mesh :position))
              rotation (api.core/clone (j/get mesh :rotation))
              depth (api.core/get-node-attr mesh :depth)
              size (api.core/get-node-attr mesh :size)
              text (api.core/get-node-attr mesh :text)
              name (j/get mesh :immersa-id)
              opts (merge
                     {:mat material
                      :depth depth
                      :size size
                      :position position
                      :rotation rotation
                      :text text}
                     data)
              opts (cond
                     (<= (:depth data) 0)
                     (assoc opts :depth 0.01)

                     (<= (:size data) 0)
                     (assoc opts :size 0.1)

                     :else opts)
              mesh (cond
                     (:depth data) (j/assoc-in! mesh [:scaling :z] (:depth data))
                     (:size data) (-> mesh
                                      (j/assoc-in! [:scaling :x] (:size data))
                                      (j/assoc-in! [:scaling :y] (:size data)))
                     (:text data) (let [x (j/get-in mesh [:scaling :x])
                                        y (j/get-in mesh [:scaling :y])
                                        z (j/get-in mesh [:scaling :z])]
                                    (api.core/dispose name)
                                    (api.mesh/text name opts)
                                    (some-> (api.mesh/text name opts)
                                            (j/assoc-in! [:scaling :x] x)
                                            (j/assoc-in! [:scaling :y] y)
                                            (j/assoc-in! [:scaling :z] z))))]
          (when (:size data)
            (slide/update-slide-data mesh :scale [(:size opts)
                                                  (:size opts)
                                                  (j/get-in mesh [:scaling :z])]))
          (when (:depth data)
            (slide/update-slide-data mesh :scale [(j/get-in mesh [:scaling :x])
                                                  (j/get-in mesh [:scaling :y])
                                                  (:depth opts)]))
          (when (:text data)
            (slide/update-slide-data mesh :text (:text opts))
            (j/call-in api.core/db [:gizmo :manager :attachToMesh] mesh)))))
    500))

(defmethod handle-ui-update :update-selected-mesh-text-content [{{:keys [value]} :data}]
  (update-text-mesh {:text value}))

(defmethod handle-ui-update :update-selected-mesh-text-depth-or-size [{{:keys [update value]} :data}]
  (update-text-mesh (hash-map update value)))

(defmethod handle-ui-update :update-gizmo-visibility [{{:keys [update value]} :data}]
  (case update
    :position (j/assoc! (api.core/gizmo-manager) :positionGizmoEnabled value)
    :rotation (j/assoc! (api.core/gizmo-manager) :rotationGizmoEnabled value)
    :scale (j/assoc! (api.core/gizmo-manager) :scaleGizmoEnabled value)))

(defmethod handle-ui-update :resize [_]
  (j/call (api.core/get-engine) :resize))

(defmethod handle-ui-update :go-to-slide [{{:keys [index]} :data}]
  (api.core/clear-selected-mesh)
  (api.camera/switch-camera-if-needed (api.core/get-scene))
  (slide/go-to-slide index))

(defmethod handle-ui-update :add-slide [_]
  (slide/add-slide))

(defmethod handle-ui-update :add-text-mesh [_]
  (let [camera (api.camera/active-camera)
        forward (j/get (j/call camera :getForwardRay) :direction)
        scaled-forward (j/call forward :scale 10)
        new-pos (j/call (j/get camera :position) :add scaled-forward)
        params {:type :text3D
                :text "Text"
                :position new-pos
                :scale (v3 1)
                :visibility 1.0
                :emissive-intensity 1.0
                :roughness 1.0
                :metallic 0.0
                :depth 0.01
                :size 1.0
                :color (api.const/color-white)}
        mesh (api.mesh/text (str (random-uuid)) params)
        direction (-> (j/get mesh :position)
                      (j/call :subtract (j/get camera :position))
                      (j/call :normalize))
        target-position (-> (j/get mesh :position)
                            (j/call :add direction))]
    (j/call mesh :lookAt target-position)
    (j/assoc-in! mesh [:rotation :x] 0)
    (j/assoc-in! mesh [:rotation :z] 0)
    (slide/add-slide-data mesh (-> params
                                   (update :position api.core/v3->v)
                                   (assoc :rotation (api.core/v3->v (j/get mesh :rotation)))
                                   (update :scale api.core/v3->v)
                                   (update :color api.core/color->v)))
    (j/call-in api.core/db [:gizmo :manager :attachToMesh] mesh)))

(defn init-ui-update-listener []
  (go-loop-sub event-bus-pub :get-ui-update [_ data]
    (handle-ui-update data)))
