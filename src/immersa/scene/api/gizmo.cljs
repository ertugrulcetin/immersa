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
      "glb" (dispatch [::events/set-selected-glb-data (merge pos-rot-scale-name {:type "glb"})])
      "image" (dispatch [::events/set-selected-image-data (merge pos-rot-scale-name {:type "image"})]))))

(defn- notify-ui-selected-mesh [mesh]
  (let [name (j/get mesh :immersa-id)
        type (api.core/get-object-type-by-name name)]
    (update-ui-by-selected-mesh-type name type mesh)))

(defn- notify-ui-selected-mesh-rotation-axis [axis value]
  (dispatch [::events/update-selected-mesh-rotation-axis axis (-> value
                                                                  api.core/to-deg
                                                                  common.utils/number->fixed
                                                                  str)]))

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

(defn- create-rotation-gizmo-on-drag-start [axis]
  (fn []
    (let [last-rotation-kw (case axis
                             :x :last-rotation-x
                             :y :last-rotation-y
                             :z :last-rotation-z)]
      (when-let [mesh (api.core/selected-mesh)]
        (when-let [initial-rotation (j/get mesh :initial-rotation)]
          (let [mesh-rotation (j/get mesh :rotation)]
            (j/assoc! mesh
                      :initial-rotation (api.core/set-v3 initial-rotation
                                                         (j/get mesh-rotation :x)
                                                         (j/get mesh-rotation :y)
                                                         (j/get mesh-rotation :z))
                      :accumulated-rotation 0
                      last-rotation-kw (j/get mesh-rotation axis))))))))

(defn create-rotation-gizmo-on-drag [axis]
  (fn []
    (when-let [mesh (api.core/selected-mesh)]
      (when (j/get mesh :initial-rotation)
        (let [last-rotation-kw (case axis
                                 :x :last-rotation-x
                                 :y :last-rotation-y
                                 :z :last-rotation-z)
              current-rotation-axis (j/get-in mesh [:rotation axis])
              rotation-change (- current-rotation-axis (j/get mesh last-rotation-kw))
              rotation-change (cond (> rotation-change Math/PI)
                                    (- rotation-change (* 2 Math/PI))

                                    (< rotation-change (- Math/PI))
                                    (+ rotation-change (* 2 Math/PI))

                                    :else rotation-change)
              accumulated-rotation-axis (+ (j/get mesh :accumulated-rotation) rotation-change)]
          (j/assoc! mesh :accumulated-rotation accumulated-rotation-axis)
          (j/assoc! mesh last-rotation-kw current-rotation-axis)
          (notify-ui-selected-mesh-rotation-axis axis (+ (j/get-in mesh [:initial-rotation axis])
                                                         (j/get mesh :accumulated-rotation))))))))

(defn create-rotation-gizmo-on-drag-end [axis]
  (fn []
    (when-let [mesh (api.core/selected-mesh)]
      (j/assoc-in! mesh [:rotation axis] (+ (j/get-in mesh [:initial-rotation axis]) (j/get mesh :accumulated-rotation)))
      (let [[axis1 axis2] (case axis
                            :x [:y :z]
                            :y [:x :z]
                            :z [:x :y])]
        (j/assoc-in! mesh [:rotation axis1] (j/get-in mesh [:initial-rotation axis1]))
        (j/assoc-in! mesh [:rotation axis2] (j/get-in mesh [:initial-rotation axis2])))
      (j/assoc-in! mesh [:initial-rotation axis] (j/get-in mesh [:rotation axis]))
      (notify-ui-selected-mesh mesh)
      (slide/update-slide-data mesh :rotation (api.core/v3->v (j/get mesh :rotation))))))

(defn- create-rotation-gizmo-drag-observables [gizmo-manager]
  (j/call-in gizmo-manager [:gizmos :rotationGizmo :xGizmo :dragBehavior :onDragStartObservable :add]
             (create-rotation-gizmo-on-drag-start :x))
  (j/call-in gizmo-manager [:gizmos :rotationGizmo :xGizmo :dragBehavior :onDragObservable :add]
             (create-rotation-gizmo-on-drag :x))
  (j/call-in gizmo-manager [:gizmos :rotationGizmo :xGizmo :dragBehavior :onDragEndObservable :add]
             (create-rotation-gizmo-on-drag-end :x))

  (j/call-in gizmo-manager [:gizmos :rotationGizmo :yGizmo :dragBehavior :onDragStartObservable :add]
             (create-rotation-gizmo-on-drag-start :y))
  (j/call-in gizmo-manager [:gizmos :rotationGizmo :yGizmo :dragBehavior :onDragObservable :add]
             (create-rotation-gizmo-on-drag :y))
  (j/call-in gizmo-manager [:gizmos :rotationGizmo :yGizmo :dragBehavior :onDragEndObservable :add]
             (create-rotation-gizmo-on-drag-end :y))

  (j/call-in gizmo-manager [:gizmos :rotationGizmo :zGizmo :dragBehavior :onDragStartObservable :add]
             (create-rotation-gizmo-on-drag-start :z))
  (j/call-in gizmo-manager [:gizmos :rotationGizmo :zGizmo :dragBehavior :onDragObservable :add]
             (create-rotation-gizmo-on-drag :z))
  (j/call-in gizmo-manager [:gizmos :rotationGizmo :zGizmo :dragBehavior :onDragEndObservable :add]
             (create-rotation-gizmo-on-drag-end :z)))

(defn- add-drag-observables [gizmo-manager]
  (let [f (fn []
            (some-> (j/get-in api.core/db [:gizmo :selected-mesh]) notify-ui-selected-mesh))]
    (j/call-in gizmo-manager [:gizmos :positionGizmo :onDragObservable :add] f)
    (j/call-in gizmo-manager [:gizmos :positionGizmo :onDragEndObservable :add]
               (fn []
                 (f)
                 (let [mesh (j/get-in api.core/db [:gizmo :selected-mesh])]
                   (some-> mesh (slide/update-slide-data :position (api.core/v3->v (j/get mesh :position)))))))
    (create-rotation-gizmo-drag-observables gizmo-manager)))

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
