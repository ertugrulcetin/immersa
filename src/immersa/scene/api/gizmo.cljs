(ns immersa.scene.api.gizmo
  (:require
    ["@babylonjs/core/Gizmos/gizmoManager" :refer [GizmoManager]]
    [applied-science.js-interop :as j]
    [immersa.common.utils :as common.utils]
    [immersa.scene.api.core :as api.core]
    [immersa.scene.macros :as m]
    [immersa.scene.slide :as slide]
    [immersa.scene.utils :as utils]
    [immersa.ui.editor.events :as events]
    [re-frame.core :refer [dispatch]]))

(def hl)

(def outline-color (api.core/color 0.3 0.74 0.94))

(defn- render-outline-selected-mesh [mesh]
  (when-let [child-meshes (j/call mesh :getChildMeshes)]
    (j/call child-meshes :forEach #(j/call hl :addMesh % outline-color)))
  (j/call hl :addMesh mesh outline-color))

(defn- update-ui-by-selected-mesh-type [name type mesh]
  (let [pos-rot-scale-name (assoc (utils/v3->v-data mesh [:position :rotation :scaling]) :name name)]
    (case type
      "text3D" (dispatch [::events/set-selected-text3D-data
                          (merge
                            pos-rot-scale-name
                            {:type "text3D"
                             :text (api.core/get-node-attr mesh :text)
                             :depth (api.core/get-node-attr mesh :depth)
                             :size (api.core/get-node-attr mesh :size)
                             :opacity (j/get mesh :visibility)
                             :color (-> (j/get-in mesh [:material :albedoColor]) api.core/color->v)
                             :emissive-color (some-> (j/get-in mesh [:material :emissiveColor]) api.core/color->v)
                             :emissive-intensity (j/get-in mesh [:material :emissiveIntensity])
                             :alpha (j/get-in mesh [:material :alpha])
                             :metallic (j/get-in mesh [:material :metallic])
                             :roughness (j/get-in mesh [:material :roughness])})])
      "glb" (dispatch [::events/set-selected-glb-data (merge pos-rot-scale-name {:type "glb"})]))))

(defn- notify-ui-selected-mesh [mesh]
  (let [name (j/get mesh :immersa-id)
        type (api.core/get-object-type-by-name name)]
    (update-ui-by-selected-mesh-type name type mesh)))

(def bb-types #{"text3D" "image"})

(defn- on-attached-to-mesh [mesh]
  (j/call hl :removeAllMeshes)
  (some-> (api.core/selected-mesh) (j/assoc! :showBoundingBox false))
  (j/assoc-in! api.core/db [:gizmo :selected-mesh] mesh)
  (when mesh
    (if (bb-types (api.core/get-object-type-by-name (j/get mesh :immersa-id)))
      (j/assoc! mesh :showBoundingBox true)
      (render-outline-selected-mesh mesh))
    (notify-ui-selected-mesh mesh))
  (when-not mesh
    (dispatch [::events/clear-selected-mesh])))

(defn- add-drag-observables [gizmo-manager]
  (let [f (fn []
            (some-> (j/get-in api.core/db [:gizmo :selected-mesh]) notify-ui-selected-mesh))]
    (j/call-in gizmo-manager [:gizmos :positionGizmo :onDragEndObservable :add]
               (fn []
                 (f)
                 (let [mesh (j/get-in api.core/db [:gizmo :selected-mesh])]
                   (some-> mesh (slide/update-slide-data :position (api.core/v3->v (j/get mesh :position)))))))
    (j/call-in gizmo-manager [:gizmos :positionGizmo :onDragObservable :add] f)
    (j/call-in gizmo-manager [:gizmos :rotationGizmo :onDragObservable :add] f)
    (j/call-in gizmo-manager [:gizmos :rotationGizmo :onDragEndObservable :add]
               (fn []
                 (f)
                 (let [mesh (j/get-in api.core/db [:gizmo :selected-mesh])]
                   (some-> mesh (slide/update-slide-data :rotation (api.core/v3->v (j/get mesh :rotation)))))))))

(defn init-gizmo-manager []
  (let [gizmo-manager (GizmoManager. (api.core/get-scene))]
    (set! hl (api.core/highlight-layer "outline-highlight-layer"
                                       :stroke? true
                                       :main-texture-ratio 1
                                       :inner-glow? true
                                       :outer-glow? false))
    ;; TODO add more meshes to exclude
    (j/call hl :addExcludedMesh (api.core/get-object-by-name "ground"))
    (j/call hl :addExcludedMesh (j/get-in api.core/db [:environment-helper :skybox]))
    (j/call hl :addExcludedMesh (j/get-in api.core/db [:environment-helper :ground]))
    (m/assoc! gizmo-manager
              :positionGizmoEnabled true
              :rotationGizmoEnabled true
              :gizmos.rotationGizmo.updateGizmoRotationToMatchAttachedMesh false
              :gizmos.positionGizmo.updateGizmoRotationToMatchAttachedMesh false
              :gizmos.positionGizmo.xGizmo.scaleRatio 1.5
              :gizmos.positionGizmo.yGizmo.scaleRatio 1.5
              :gizmos.positionGizmo.zGizmo.scaleRatio 1.5)
    (add-drag-observables gizmo-manager)
    (j/call-in gizmo-manager [:onAttachedToMeshObservable :add] #(on-attached-to-mesh %))
    (j/assoc-in! api.core/db [:gizmo :manager] gizmo-manager)
    gizmo-manager))
