(ns immersa.scene.api.gizmo
  (:require
    ["@babylonjs/core/Behaviors/Meshes/pointerDragBehavior" :refer [PointerDragBehavior]]
    ["@babylonjs/core/Gizmos/gizmoManager" :refer [GizmoManager]]
    [applied-science.js-interop :as j]
    [immersa.scene.api.core :as api.core]
    [immersa.scene.macros :as m]
    [immersa.scene.slide :as slide]
    [immersa.scene.ui-notifier :as ui.notifier]
    [immersa.scene.utils :as utils]
    [immersa.ui.editor.events :as events]
    [re-frame.core :refer [dispatch]]))

(def hl)

(def outline-color-linked (api.core/color 0.3 1.0 0.5))
(def outline-color-unlinked (api.core/color 1.0 0.2 0.3))

(defn- get-linked-type [mesh]
  (let [prev-index (dec @slide/current-slide-index)
        next-index (inc @slide/current-slide-index)
        slides-count (count @slide/all-slides)]
    (cond
      (and (= 0 @slide/current-slide-index)
           (slide/get-slide-data-by-index next-index mesh :type))
      :next-linked

      (and (= (dec slides-count) @slide/current-slide-index)
           (slide/get-slide-data-by-index prev-index mesh :type))
      :prev-linked

      (or (< prev-index 0)
          (>= next-index slides-count))
      :unlinked

      (and (slide/get-slide-data-by-index prev-index mesh :type)
           (slide/get-slide-data-by-index next-index mesh :type))
      :both-linked

      (slide/get-slide-data-by-index prev-index mesh :type)
      :prev-linked

      (slide/get-slide-data-by-index next-index mesh :type)
      :next-linked

      :else :unlinked)))

(defn- render-outline-selected-mesh [mesh linked-type]
  (let [color (if (= linked-type :unlinked)
                outline-color-unlinked
                outline-color-linked)]
    (when-let [child-meshes (j/call mesh :getChildMeshes)]
      (j/call child-meshes :forEach #(j/call hl :addMesh % color)))
    (j/call hl :addMesh mesh color)))

(def bb-types #{"text3D" "image"})

(defn- on-attached-to-mesh [mesh]
  (j/call hl :removeAllMeshes)
  (some-> (api.core/selected-mesh) (j/assoc! :showBoundingBox false))
  (some-> (api.core/selected-mesh) api.core/detach-pointer-drag-behav)
  (j/assoc-in! api.core/db [:gizmo :selected-mesh] mesh)
  (when mesh
    (let [linked-type (get-linked-type mesh)
          type (api.core/get-object-type-by-name (j/get mesh :immersa-id))]
      (when (= type "text3D")
        (j/assoc! (api.core/gizmo-manager) :scaleGizmoEnabled false)
        (ui.notifier/notify-gizmo-state :scale false))
      (when (and (j/get (api.core/gizmo-manager) :positionGizmoEnabled)
                 (slide/camera-locked?))
        (api.core/attach-pointer-drag-behav mesh))
      (if (bb-types type)
        (do
          (if (= linked-type :unlinked)
            (j/assoc-in! (api.core/get-bb-renderer) [:frontColor] outline-color-unlinked)
            (j/assoc-in! (api.core/get-bb-renderer) [:frontColor] outline-color-linked))
          (j/assoc! mesh :showBoundingBox true))
        (render-outline-selected-mesh mesh linked-type))
      (ui.notifier/notify-ui-selected-mesh (j/assoc! mesh :linked-type linked-type))))
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
          (ui.notifier/notify-ui-selected-mesh-rotation-axis axis (+ (j/get-in mesh [:initial-rotation axis])
                                                                     (j/get mesh :accumulated-rotation))))))))

(defn create-rotation-gizmo-on-drag-end [axis]
  (fn []
    (when-let [mesh (api.core/selected-mesh)]
      (j/assoc-in! mesh [:rotation axis] (+ (j/get-in mesh [:initial-rotation axis]) (j/get mesh :accumulated-rotation)))
      #_(let [[axis1 axis2] (case axis
                              :x [:y :z]
                              :y [:x :z]
                              :z [:x :y])]
          (j/assoc-in! mesh [:rotation axis1] (j/get-in mesh [:initial-rotation axis1]))
          (j/assoc-in! mesh [:rotation axis2] (j/get-in mesh [:initial-rotation axis2])))
      (j/assoc-in! mesh [:initial-rotation axis] (j/get-in mesh [:rotation axis]))
      (ui.notifier/notify-ui-selected-mesh mesh)
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

(defn- notify-ui-selected-mesh []
  (some-> (api.core/selected-mesh) ui.notifier/notify-ui-selected-mesh))

(defn- update-slide-data-for-position []
  (let [mesh (api.core/selected-mesh)]
    (notify-ui-selected-mesh)
    (some-> mesh (slide/update-slide-data :position (api.core/v3->v (j/get mesh :position))))))

(defn- add-drag-observables [gizmo-manager]
  (let [f notify-ui-selected-mesh]
    (j/call-in gizmo-manager [:gizmos :positionGizmo :onDragObservable :add] f)
    (j/call-in gizmo-manager [:gizmos :positionGizmo :onDragEndObservable :add]
               (fn []
                 (f)
                 (update-slide-data-for-position)))
    (j/call-in gizmo-manager [:gizmos :scaleGizmo :onDragObservable :add] f)
    (j/call-in gizmo-manager [:gizmos :scaleGizmo :onDragEndObservable :add]
               (fn []
                 (f)
                 (let [mesh (api.core/selected-mesh)]
                   (some-> mesh (slide/update-slide-data :scaling (api.core/v3->v (j/get mesh :scaling)))))))
    (create-rotation-gizmo-drag-observables gizmo-manager)))

(defn toggle-gizmo [type]
  (let [gizmo (case type
                :position :positionGizmoEnabled
                :rotation :rotationGizmoEnabled
                :scale :scaleGizmoEnabled)
        enabled? (not (j/get-in api.core/db [:gizmo :manager gizmo]))]
    (j/assoc-in! api.core/db [:gizmo :manager gizmo] enabled?)
    (when (= type :position)
      (when-let [mesh (api.core/selected-mesh)]
        (if (and enabled? (slide/camera-locked?))
          (api.core/attach-pointer-drag-behav mesh)
          (api.core/detach-pointer-drag-behav mesh))))
    (ui.notifier/notify-gizmo-state type enabled?)))

(defn- init-pointer-drag-behaviour []
  (let [pdb (j/assoc! (PointerDragBehavior.)
                      :useObjectOrientationForDragging false
                      :updateDragPlane false)]
    (j/call-in pdb [:onDragObservable :add] notify-ui-selected-mesh)
    (j/call-in pdb [:onDragEndObservable :add] update-slide-data-for-position)
    pdb))

(defn init-gizmo-manager []
  (let [gizmo-manager (GizmoManager. (api.core/get-scene) 3)
        pointer-drag-behaviour (init-pointer-drag-behaviour)]
    (set! hl (api.core/highlight-layer "outline-highlight-layer"
                                       :stroke? true
                                       :main-texture-ratio 1
                                       :inner-glow? true
                                       :outer-glow? false))
    ;; TODO add more meshes to exclude
    (some->> (api.core/get-object-by-name "ground") (j/call hl :addExcludedMesh))
    (j/call hl :addExcludedMesh (j/get-in api.core/db [:environment-helper :skybox]))
    (j/call hl :addExcludedMesh (j/get-in api.core/db [:environment-helper :ground]))
    (m/assoc! gizmo-manager
              :positionGizmoEnabled true
              :rotationGizmoEnabled true
              :scaleGizmoEnabled true
              :gizmos.rotationGizmo.updateGizmoRotationToMatchAttachedMesh false
              :gizmos.positionGizmo.updateGizmoRotationToMatchAttachedMesh false
              :gizmos.positionGizmo.xGizmo.scaleRatio 1.5
              :gizmos.positionGizmo.yGizmo.scaleRatio 1.5
              :gizmos.positionGizmo.zGizmo.scaleRatio 1.5
              ;; :gizmos.positionGizmo.xGizmo._gizmoMesh.rotation.y (/ Math/PI 2)
              :gizmos.positionGizmo.zGizmo._gizmoMesh.rotation.y Math/PI
              :gizmos.rotationGizmo.xGizmo.scaleRatio 1.2
              :gizmos.rotationGizmo.yGizmo.scaleRatio 1.2
              :gizmos.rotationGizmo.zGizmo.scaleRatio 1.2
              :gizmos.positionGizmo.planarGizmoEnabled true
              :gizmos.positionGizmo.xPlaneGizmo._gizmoMesh.position.y 0.05
              :gizmos.positionGizmo.xPlaneGizmo._gizmoMesh.position.z -0.05
              :gizmos.positionGizmo.yPlaneGizmo._gizmoMesh.position.x 0.05
              :gizmos.positionGizmo.yPlaneGizmo._gizmoMesh.position.z -0.05
              :gizmos.positionGizmo.zPlaneGizmo._gizmoMesh.position.x 0.05
              :gizmos.positionGizmo.zPlaneGizmo._gizmoMesh.position.y 0.05)
    (add-drag-observables gizmo-manager)
    (j/assoc! gizmo-manager
              :rotationGizmoEnabled false
              :scaleGizmoEnabled false)
    (j/call-in gizmo-manager [:onAttachedToMeshObservable :add] #(if (j/get % :hit-box?)
                                                                   (api.core/attach-to-mesh (j/get % :parent))
                                                                   (on-attached-to-mesh %)))
    (j/assoc-in! api.core/db [:gizmo :manager] gizmo-manager)
    (j/assoc! api.core/db :pointer-drag-behaviour pointer-drag-behaviour)
    gizmo-manager))
