(ns immersa.scene.ui-listener
  (:require
    [applied-science.js-interop :as j]
    [goog.functions :as functions]
    [immersa.common.communication :refer [event-bus-pub]]
    [immersa.scene.api.camera :as api.camera]
    [immersa.scene.api.core :as api.core]
    [immersa.scene.api.mesh :as api.mesh])
  (:require-macros
    [immersa.common.macros :refer [go-loop-sub]]))

(defmulti handle-ui-update :type)

(defmethod handle-ui-update :update-selected-mesh [{{:keys [update value]} :data}]
  (when-let [mesh (j/get-in api.core/db [:gizmo :selected-mesh])]
    (case update
      :position (j/assoc! mesh :position (api.core/v->v3 value))
      :rotation (j/assoc! mesh :rotation (api.core/v->v3 (mapv api.core/to-rad value)))
      :scaling (j/assoc! mesh :scaling (api.core/v->v3 value)))))

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
    (j/assoc! ground-material :primaryColor new-color)))

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
      (j/assoc-in! mesh [:material :emissiveColor] new-color))))

(defmethod handle-ui-update :update-selected-mesh-emissive-color [{{:keys [value]} :data}]
  (when-let [mesh (j/get-in api.core/db [:gizmo :selected-mesh])]
    (j/assoc-in! mesh [:material :emissiveColor] (apply api.core/color-rgb value))))

(def update-text-mesh
  (functions/debounce
    (fn [data]
      (when-let [mesh (j/get-in api.core/db [:gizmo :selected-mesh])]
        (let [material (api.core/clone (j/get mesh :material))
              position (api.core/clone (j/get mesh :position))
              rotation (api.core/clone (j/get mesh :rotation))
              scaling (api.core/clone (j/get mesh :scaling))
              depth (api.core/get-node-attr mesh :depth)
              size (api.core/get-node-attr mesh :size)
              text (api.core/get-node-attr mesh :text)
              name (j/get mesh :immersa-id)
              _ (api.core/dispose name)
              opts (merge
                     {:mat material
                      :depth depth
                      :size size
                      :position position
                      :rotation rotation
                      :scale scaling
                      :text text}
                     data)
              opts (cond
                     (= (:depth data) 0)
                     (assoc opts :depth 0.01)

                     (= (:size data) 0)
                     (assoc opts :size 0.1)

                     :else opts)
              mesh (api.mesh/text name opts)]
          (j/call-in api.core/db [:gizmo :manager :attachToMesh] mesh))))
    500))

(defmethod handle-ui-update :update-selected-mesh-text-content [{{:keys [value]} :data}]
  (update-text-mesh {:text value}))

(defmethod handle-ui-update :update-selected-mesh-text-depth-or-size [{{:keys [update value]} :data}]
  (update-text-mesh (hash-map update value)))

(defmethod handle-ui-update :resize [_]
  (j/call (api.core/get-engine) :resize))

(defn init-ui-update-listener []
  (go-loop-sub event-bus-pub :get-ui-update [_ data]
    (handle-ui-update data)))
