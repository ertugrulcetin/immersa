(ns immersa.scene.api.gizmo
  (:require
    ["@babylonjs/core/Gizmos/gizmoManager" :refer [GizmoManager]]
    [applied-science.js-interop :as j]
    [immersa.scene.api.core :as api.core]
    [immersa.scene.macros :as m]
    [immersa.ui.editor.events :as events]
    [re-frame.core :refer [dispatch]]))

(def hl)

(def outline-color (api.core/color 0.3 0.74 0.94))

(defn clear-selected-mesh []
  (j/call-in api.core/db [:gizmo :manager :attachToMesh] nil))

(defn- render-outline-selected-mesh [mesh]
  (when-let [child-meshes (j/call mesh :getChildMeshes)]
    (j/call child-meshes :forEach #(j/call hl :addMesh % outline-color)))
  (j/call hl :addMesh mesh outline-color))

(defn number->fixed [n]
  (j/call n :toFixed 2))

(defn- notify-ui-selected-mesh [mesh]
  (let [name (j/get mesh :immersa-id)
        position (mapv number->fixed (api.core/v3->v (j/get mesh :position)))
        rotation (mapv (comp number->fixed api.core/to-deg) (api.core/v3->v (j/get mesh :rotation)))
        scaling (mapv number->fixed (api.core/v3->v (j/get mesh :scaling)))]
    (dispatch [::events/set-selected-mesh {:name name
                                           :position position
                                           :rotation rotation
                                           :scaling scaling}])))

(defn- on-attached-to-mesh [mesh]
  (j/call hl :removeAllMeshes)
  (j/assoc-in! api.core/db [:gizmo :selected-mesh] mesh)
  (when mesh
    (render-outline-selected-mesh mesh)
    (notify-ui-selected-mesh mesh))
  (when-not mesh
    (dispatch [::events/clear-selected-mesh])))

(defn- add-drag-observables [gizmo-manager]
  (let [f (fn [_]
            (some-> (j/get-in api.core/db [:gizmo :selected-mesh]) notify-ui-selected-mesh))]
    (j/call-in gizmo-manager [:gizmos :positionGizmo :onDragEndObservable :add] f)
    (j/call-in gizmo-manager [:gizmos :positionGizmo :onDragObservable :add] f)
    (j/call-in gizmo-manager [:gizmos :rotationGizmo :onDragObservable :add] f)
    (j/call-in gizmo-manager [:gizmos :rotationGizmo :onDragEndObservable :add] f)))

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
