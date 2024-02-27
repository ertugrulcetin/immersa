(ns immersa.scene.ui-listener
  (:require
    ["@babylonjs/core/Maths/math" :refer [Vector3]]
    [applied-science.js-interop :as j]
    [com.rpl.specter :as sp]
    [goog.functions :as functions]
    [immersa.common.communication :refer [event-bus-pub]]
    [immersa.scene.api.camera :as api.camera]
    [immersa.scene.api.component :as api.component]
    [immersa.scene.api.constant :as api.const]
    [immersa.scene.api.core :as api.core :refer [v3]]
    [immersa.scene.api.mesh :as api.mesh]
    [immersa.scene.slide :as slide]
    [immersa.scene.ui-notifier :as ui.notifier]
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
        brightness (slide/calculate-brightness-factor (slide/get-slide-data :skybox [:background :brightness]))
        new-color (apply api.core/color-rgb (map (partial * brightness) value))]
    (j/assoc! skybox-material :primaryColor new-color)
    (j/assoc! ground-material :primaryColor new-color)
    (slide/update-slide-data :skybox [:background :color] value)
    (ui.notifier/sync-slides-info @slide/current-slide-index @slide/all-slides)))

(defmethod handle-ui-update :update-background-brightness [{{:keys [value]} :data}]
  (let [skybox-material (j/get-in api.core/db [:environment-helper :skybox :material])
        ground-material (j/get-in api.core/db [:environment-helper :ground :material])
        color (slide/get-slide-data :skybox [:background :color])
        brightness (slide/calculate-brightness-factor value)
        new-color (apply api.core/color-rgb (map (partial * brightness) color))]
    (j/assoc! skybox-material :primaryColor new-color)
    (j/assoc! ground-material :primaryColor new-color)
    (slide/update-slide-data :skybox [:background :brightness] value)
    (ui.notifier/sync-slides-info @slide/current-slide-index @slide/all-slides)))

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

(defn update-text-mesh [data]
  (when-let [mesh (j/get-in api.core/db [:gizmo :selected-mesh])]
    (let [material (api.core/clone (j/get mesh :material))
          position (api.core/clone (j/get mesh :position))
          rotation (api.core/clone (j/get mesh :rotation))
          scale (api.core/clone (j/get mesh :scaling))
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
                  :scale scale
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
                 (:text data) (do
                                (api.core/dispose name)
                                (api.mesh/text name opts)))]
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

(def update-text-mesh-with-debounce (functions/debounce update-text-mesh 500))

(defmethod handle-ui-update :update-selected-mesh-text-content [{{:keys [value]} :data}]
  (update-text-mesh-with-debounce {:text value}))

(defmethod handle-ui-update :update-selected-mesh-text-depth-or-size [{{:keys [update value]} :data}]
  (update-text-mesh (hash-map update value)))

(defmethod handle-ui-update :update-selected-mesh-face-to-screen? [{{:keys [value]} :data}]
  (when-let [mesh (api.core/selected-mesh)]
    (if value
      (let [rotation [0 0 0]]
        (slide/update-slide-data mesh :rotation rotation)
        (j/assoc! mesh
                  :rotation (api.core/v->v3 rotation)
                  :billboardMode api.const/mesh-billboard-mode-all))
      (j/assoc! mesh :billboardMode api.const/mesh-billboard-mode-none))
    (slide/update-slide-data mesh :face-to-screen? value)
    (api.core/update-node-attr mesh :face-to-screen? value)
    (ui.notifier/notify-ui-selected-mesh mesh)))

(defmethod handle-ui-update :update-selected-image-mesh-transparent? [{{:keys [value]} :data}]
  (when-let [mesh (api.core/selected-mesh)]
    (j/assoc-in! mesh [:material :diffuseTexture :hasAlpha] value)
    (slide/update-slide-data mesh :transparent? value)
    (api.core/update-node-attr mesh :transparent? value)
    (ui.notifier/notify-ui-selected-mesh mesh)))

(defmethod handle-ui-update :update-gizmo-visibility [{{:keys [update value]} :data}]
  (case update
    :position (j/assoc! (api.core/gizmo-manager) :positionGizmoEnabled value)
    :rotation (j/assoc! (api.core/gizmo-manager) :rotationGizmoEnabled value)
    :scale (j/assoc! (api.core/gizmo-manager) :scaleGizmoEnabled value)))

(defmethod handle-ui-update :resize [_]
  (api.core/resize))

(defmethod handle-ui-update :go-to-slide [{{:keys [index]} :data}]
  (api.core/clear-selected-mesh)
  (slide/go-to-slide index))

(defmethod handle-ui-update :add-slide [_]
  (slide/add-slide))

(defmethod handle-ui-update :delete-slide [_]
  (slide/delete-slide))

(defmethod handle-ui-update :re-order-slides [{{:keys [value]} :data}]
  (let [[old-index new-index] value
        old-index-slide (get @slide/all-slides old-index)
        selected-slide-id (get-in @slide/all-slides [@slide/current-slide-index :id])
        _ (sp/setval [sp/ATOM old-index] sp/NONE slide/all-slides)
        _ (sp/transform sp/ATOM #(slide/vec-insert % old-index-slide new-index) slide/all-slides)
        current-index (first (sp/select-one [sp/INDEXED-VALS #(= selected-slide-id (:id (second %)))] @slide/all-slides))]
    (reset! slide/current-slide-index current-index)
    (ui.notifier/sync-slides-info @slide/current-slide-index @slide/all-slides)))

(defmethod handle-ui-update :create-slide-thumbnail [_]
  (slide/update-thumbnail))

(defmethod handle-ui-update :toggle-camera-lock [{{:keys [value]} :data}]
  (slide/update-slide-data :camera :locked? value)
  (api.camera/toggle-camera-lock value)
  (ui.notifier/notify-camera-lock-state value))

(defn get-pos-from-camera-dir []
  (let [camera (api.camera/active-camera)
        forward (j/get (j/call camera :getForwardRay) :direction)
        scaled-forward (j/call forward :scale 10)]
    (j/call (j/get camera :position) :add scaled-forward)))

(defmethod handle-ui-update :add-text-mesh [_]
  (let [new-pos (get-pos-from-camera-dir)
        params {:type :text3D
                :text "Text"
                :face-to-screen? true
                :position new-pos
                :rotation (v3)
                :scale (v3 1)
                :visibility 1.0
                :emissive-intensity 1.0
                :roughness 1.0
                :metallic 0.0
                :depth 0.01
                :size 1.0
                :color (api.const/color-black)}
        mesh (api.mesh/text (str (random-uuid)) params)
        #_#_direction (-> (j/get mesh :position)
                          (j/call :subtract (j/get camera :position))
                          (j/call :normalize))
        #_#_target-position (-> (j/get mesh :position)
                                (j/call :add direction))]
    ;; (j/call mesh :lookAt target-position)
    ;; (j/assoc-in! mesh [:rotation :x] 0)
    ;; (j/assoc-in! mesh [:rotation :z] 0)
    (slide/add-slide-data mesh (-> params
                                   (update :position api.core/v3->v)
                                   ;; (assoc :rotation (api.core/v3->v (j/get mesh :rotation))
                                   (update :rotation api.core/v3->v)
                                   (update :scale api.core/v3->v)
                                   (update :color api.core/color->v)))
    (j/call-in api.core/db [:gizmo :manager :attachToMesh] mesh)))

(defmethod handle-ui-update :add-image [{{:keys [value]} :data}]
  (let [uuid (str (random-uuid))
        texture (api.core/texture value)]
    (j/call-in texture [:onLoadObservable :add]
               (fn [texture]
                 (let [new-pos (get-pos-from-camera-dir)
                       params {:type :image
                               :texture texture
                               :path value
                               :face-to-screen? true
                               :position new-pos
                               :rotation (v3)
                               :scale (v3 1)
                               :visibility 1.0
                               :transparent? true}
                       mesh (api.component/image uuid params)]
                   (slide/add-slide-data mesh (-> params
                                                  (assoc :asset-type :texture)
                                                  (update :position api.core/v3->v)
                                                  (update :rotation api.core/v3->v)
                                                  (update :scale api.core/v3->v)))
                   (api.core/attach-to-mesh mesh))))))

(defn- minimize [total-x bounding-info]
  (j/call Vector3 :Minimize total-x (j/get-in bounding-info [:boundingBox :minimumWorld])))

(defn- maximize [total-x bounding-info]
  (j/call Vector3 :Maximize total-x (j/get-in bounding-info [:boundingBox :maximumWorld])))

(defmethod handle-ui-update :add-model [{{:keys [value]} :data}]
  (let [uuid (str (random-uuid))
        task (api.core/add-mesh-task
               {:name (str uuid "-mesh-task")
                :meshes-name ""
                :url value
                :on-complete
                (fn [meshes root-mesh]
                  (j/call root-mesh :computeWorldMatrix true)
                    (let [total-min (atom (v3 js/Number.POSITIVE_INFINITY))
                        total-max (atom (v3 js/Number.NEGATIVE_INFINITY))
                        _ (j/call meshes :forEach
                                  (fn [mesh]
                                    (when (j/get mesh :getBoundingInfo)
                                      (let [bounding-info (j/call mesh :getBoundingInfo)]
                                        (reset! total-min (minimize @total-min bounding-info))
                                        (reset! total-max (maximize @total-max bounding-info))))))
                        model-size (j/call @total-max :subtract @total-min)
                        model-max-dimension (js/Math.max (j/get model-size :x)
                                                         (j/get model-size :y)
                                                         (j/get model-size :z))
                        target-size 5
                        scale (/ target-size model-max-dimension)
                        params {:type :glb
                                :path value
                                :position (j/update! (get-pos-from-camera-dir) :y #(- % 2))
                                :rotation (v3)
                                :scale    (v3 scale)
                                ;; :visibility 1.0, maybe we need it
                                }
                        mesh (api.mesh/glb->mesh uuid params)]
                    (slide/add-slide-data mesh (-> params
                                                   (assoc :asset-type :model)
                                                   (update :position api.core/v3->v)
                                                   (update :rotation api.core/v3->v)
                                                   (update :scale api.core/v3->v)))
                    (api.core/attach-to-mesh mesh)))})]
    (j/call task :run (api.core/get-scene) (fn []))))

(defn init-ui-update-listener []
  (go-loop-sub event-bus-pub :get-ui-update [_ data]
    (handle-ui-update data)))
