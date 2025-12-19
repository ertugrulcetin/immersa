(ns immersa.scene.ui-listener
  (:require
    ["@babylonjs/core/Maths/math" :refer [Vector3]]
    [applied-science.js-interop :as j]
    [com.rpl.specter :as sp]
    [goog.functions :as functions]
    [immersa.common.communication :refer [event-bus-pub]]
    [immersa.common.utils :as common.utils]
    [immersa.scene.api.camera :as api.camera]
    [immersa.scene.api.component :as api.component]
    [immersa.scene.api.constant :as api.const]
    [immersa.scene.api.core :as api.core :refer [v3]]
    [immersa.scene.api.mesh :as api.mesh]
    [immersa.scene.slide :as slide]
    [immersa.scene.ui-notifier :as ui.notifier]
    [immersa.scene.undo-redo :as undo.redo]
    [immersa.scene.utils :as utils]
    [immersa.ui.editor.events :as editor.events]
    [immersa.ui.present.events :as present.events]
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
      :opacity (do
                 (if (= (api.core/get-object-type mesh) "image")
                   (j/assoc-in! mesh [:material :alpha] value)
                   (j/assoc! mesh :visibility value))
                 (slide/update-slide-data mesh :visibility value))
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

(defmethod handle-ui-update :update-selected-mesh-text-content [{{:keys [value]} :data}]
  (let [mesh (api.core/selected-mesh)
        current-text (api.core/get-node-attr mesh :text)]
    (slide/update-text-mesh-with-debounce {:text value
                                           :mesh mesh})
    (undo.redo/create-action-with-debounce {:type :update-text-content
                                            :id (api.core/get-object-name mesh)
                                            :params {:from current-text
                                                     :to value}})))

(defmethod handle-ui-update :update-selected-mesh-text-depth-or-size [{{:keys [update value]} :data}]
  (let [mesh (api.core/selected-mesh)]
    (slide/update-text-mesh (hash-map update value :mesh mesh))
    (undo.redo/create-action {:type (if (= update :size) :update-text-size :update-text-depth)
                              :id (api.core/get-object-name mesh)
                              :params {:from (api.core/get-node-attr mesh (if (= update :size) :size :depth))
                                       :to value}})))

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
    (when (api.core/get-node-attr mesh :face-to-screen?)
      (j/assoc! (api.core/gizmo-manager) :rotationGizmoEnabled false)
      (ui.notifier/notify-gizmo-state :rotation false))
    (ui.notifier/notify-ui-selected-mesh)))

(defmethod handle-ui-update :update-selected-image-mesh-transparent? [{{:keys [value]} :data}]
  (when-let [mesh (api.core/selected-mesh)]
    (j/assoc-in! mesh [:material :diffuseTexture :hasAlpha] value)
    (slide/update-slide-data mesh :transparent? value)
    (api.core/update-node-attr mesh :transparent? value)
    (ui.notifier/notify-ui-selected-mesh)))

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

(defmethod handle-ui-update :add-slide [{:keys [index]}]
  (let [[index slide] (slide/add-slide index)]
    (undo.redo/create-action {:type :duplicate-slide
                              :params {:index index
                                       :slide slide}})))

(defmethod handle-ui-update :blank-slide [_]
  (let [[index slide] (slide/blank-slide)]
    (undo.redo/create-action {:type :blank-slide
                              :params {:index index
                                       :slide slide}})))

(defmethod handle-ui-update :delete-slide [{:keys [index]}]
  (let [current-index @slide/current-slide-index
        index (or index @slide/current-slide-index)
        slide (get @slide/all-slides index)]
    (slide/delete-slide index)
    (undo.redo/create-action {:type :delete-slide
                              :params {:index index
                                       :selected-index-before current-index
                                       :slide slide}})))

(defmethod handle-ui-update :re-order-slides [{{:keys [value]} :data}]
  (let [[old-index new-index] value]
    (slide/re-order-slides old-index new-index)
    (undo.redo/create-action {:type :re-order-slides
                              :params {:old-index old-index
                                       :new-index new-index}})))

(defmethod handle-ui-update :create-slide-thumbnail [_]
  (slide/update-thumbnail))

(defmethod handle-ui-update :toggle-camera-lock [{{:keys [value]} :data}]
  (slide/update-slide-data :camera :locked? value)
  (api.camera/toggle-camera-lock value)
  (when-let [mesh (api.core/selected-mesh)]
    (if (and value (slide/camera-locked?))
      (api.core/attach-pointer-drag-behav mesh)
      (api.core/detach-pointer-drag-behav mesh)))
  (ui.notifier/notify-camera-lock-state value))

(defmethod handle-ui-update :toggle-ground-enabled [{{:keys [value]} :data}]
  (slide/update-slide-data :ground :enabled? value)
  (api.core/set-enabled (api.core/get-object-by-name "ground") value)
  (ui.notifier/notify-ground-state value))

(defn get-pos-from-camera-dir []
  (let [camera (api.camera/active-camera)
        forward (j/get (j/call camera :getForwardRay) :direction)
        scaled-forward (j/call forward :scale 10)]
    (j/call (j/get camera :position) :add scaled-forward)))

(defmethod handle-ui-update :add-text-mesh [_]
  (let [new-pos (get-pos-from-camera-dir)
        uuid (str (random-uuid))
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
        mesh (api.mesh/text uuid params)
        params (-> params
                   (update :position api.core/v3->v)
                   (update :rotation api.core/v3->v)
                   (update :scale api.core/v3->v)
                   (update :color api.core/color->v))]
    (slide/add-slide-data mesh params)
    (undo.redo/create-action {:type :create-text
                              :id uuid
                              :params params})
    (api.core/attach-to-mesh mesh)))

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
                                                  (dissoc :texture)
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
        on-complete (fn [root-mesh]
                      (j/call root-mesh :computeWorldMatrix true)
                      (let [total-min (atom (v3 js/Number.POSITIVE_INFINITY))
                            total-max (atom (v3 js/Number.NEGATIVE_INFINITY))
                            meshes (j/call root-mesh :getChildMeshes)
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
                                    :scale (v3 scale)
                                    ;; :visibility 1.0, maybe we need it
                                    }
                            mesh (api.mesh/glb->mesh uuid params)]
                        (slide/add-slide-data mesh (-> params
                                                       (assoc :asset-type :model)
                                                       (update :position api.core/v3->v)
                                                       (update :rotation api.core/v3->v)
                                                       (update :scale api.core/v3->v)))
                        (api.core/attach-to-mesh mesh)))
        prev-mesh (j/get-in api.core/db [:models value])]
    (if prev-mesh
      (on-complete prev-mesh)
      (let [task (api.core/add-mesh-task
                   {:name (str uuid "-mesh-task")
                    :meshes-name ""
                    :url value
                    :on-complete on-complete})]
        (j/call task :run (api.core/get-scene) (fn []))))))

(defmethod handle-ui-update :attach-camera-controls [_]
  (let [free-camera (api.core/get-object-by-name "free-camera")
        arc-camera (api.core/get-object-by-name "arc-camera")]
    (api.camera/attach-control free-camera)
    (api.camera/attach-control arc-camera)))

(defmethod handle-ui-update :detach-camera-controls [_]
  (let [free-camera (api.core/get-object-by-name "free-camera")
        arc-camera (api.core/get-object-by-name "arc-camera")]
    (api.camera/detach-control free-camera)
    (api.camera/detach-control arc-camera)))

(defmethod handle-ui-update :clear-selected-mesh [_]
  (api.core/clear-selected-mesh))

(defmethod handle-ui-update :update-slide-info [_]
  (dispatch [::present.events/update-slide-info @slide/current-slide-index (count @slide/all-slides)]))

(defmethod handle-ui-update :add-listeners-for-present-mode [_]
  (slide/add-listeners-for-present-mode))

(defmethod handle-ui-update :remove-listeners-for-present-mode [_]
  (common.utils/remove-element-listener js/window "keydown" slide/next-prev-slide-event-listener))

(defmethod handle-ui-update :set-presentation-id [{{:keys [id]} :data}]
  (j/assoc-in! api.core/db [:presentation :id] id))

(defmethod handle-ui-update :set-user-id [{{:keys [id]} :data}]
  (j/assoc-in! api.core/db [:user :id] id))

(defmethod handle-ui-update :create-go-to-slide-action [{{:keys [from to]} :data}]
  (when (not= from to)
    (undo.redo/create-action {:type :go-to-slide
                              :params {:from from
                                       :to to}})))

(defmethod handle-ui-update :update-present-mode [{{:keys [enabled]} :data}]
  (j/assoc! api.core/db :present? enabled)
  (j/assoc! (api.core/gizmo-manager) :enableAutoPicking (not enabled))
  (when-not enabled
    (slide/go-to-slide @slide/current-slide-index)))

(defn init-ui-update-listener []
  (go-loop-sub event-bus-pub :get-ui-update [_ data]
    (handle-ui-update data)))
