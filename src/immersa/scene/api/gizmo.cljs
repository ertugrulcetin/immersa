(ns immersa.scene.api.gizmo
  (:require
    ["@babylonjs/core/Gizmos/gizmoManager" :refer [GizmoManager]]
    [applied-science.js-interop :as j]
    [immersa.scene.api.core :as api.core]
    [immersa.scene.macros :as m]))

(def hl)

(def outline-color (api.core/color 0.3 0.74 0.94))

(defn clear-selected-mesh []
  (j/call-in api.core/db [:gizmo :manager :attachToMesh] nil))

(defn- on-attached-to-mesh [mesh]
  (j/call hl :removeAllMeshes)
  (j/assoc-in! api.core/db [:gizmo :selected-mesh] mesh)
  (when mesh
    (when-let [child-meshes (j/call mesh :getChildMeshes)]
      (j/call child-meshes :forEach #(j/call hl :addMesh % outline-color)))
    (j/call hl :addMesh mesh outline-color)))

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
              :gizmos.rotationGizmo.updateGizmoPositionToMatchAttachedMesh true
              :gizmos.positionGizmo.updateGizmoRotationToMatchAttachedMesh false
              :gizmos.positionGizmo.xGizmo.scaleRatio 1.5
              :gizmos.positionGizmo.yGizmo.scaleRatio 1.5
              :gizmos.positionGizmo.zGizmo.scaleRatio 1.5)
    (j/call-in gizmo-manager [:onAttachedToMeshObservable :add] #(on-attached-to-mesh %))
    (j/assoc-in! api.core/db [:gizmo :manager] gizmo-manager)
    gizmo-manager))
