(ns immersa.scene.slide
  (:require
    [applied-science.js-interop :as j]
    [cljs.core.async :as a :refer [go go-loop <!]]
    [clojure.set :as set]
    [immersa.common.utils :as common.utils]
    [immersa.events :as events]
    [immersa.scene.api.animation :as api.animation]
    [immersa.scene.api.camera :as api.camera]
    [immersa.scene.api.component :as api.component]
    [immersa.scene.api.constant :as api.const]
    [immersa.scene.api.core :as api.core :refer [v2 v3 v4]]
    [immersa.scene.api.gui :as api.gui]
    [immersa.scene.api.mesh :as api.mesh]
    [immersa.scene.api.particle :as api.particle]
    [re-frame.core :refer [dispatch]]))

(defn- get-position-anim [object-slide-info object-name]
  (let [object (api.core/get-object-by-name object-name)
        start-pos (j/get object :position)
        end-pos (:position object-slide-info)]
    (cond
      (and start-pos end-pos (vector? end-pos))
      [object-name (api.animation/create-multiple-position-animation {:start start-pos
                                                                      :end end-pos
                                                                      :duration (:duration object-slide-info)
                                                                      :delay (:delay object-slide-info)})]

      (and start-pos end-pos (not (api.core/equals? start-pos end-pos)))
      [object-name (api.animation/create-position-animation {:start start-pos
                                                             :end end-pos
                                                             :duration (:duration object-slide-info)
                                                             :delay (:delay object-slide-info)})]

      ;; TODO handle here
      (and (= object-name :camera) start-pos end-pos (not (api.core/equals? start-pos end-pos)))
      [object-name (api.animation/create-position-animation {:start start-pos
                                                             :end end-pos
                                                             :duration (:duration object-slide-info)
                                                             :delay (:delay object-slide-info)})]

      (and (= object-name :camera) (not end-pos) (not (api.core/equals? start-pos (j/get object :init-position))))
      [object-name (api.animation/create-position-animation {:start start-pos
                                                             :end (api.core/clone (j/get object :init-position))
                                                             :duration (:duration object-slide-info)
                                                             :delay (:delay object-slide-info)})])))

(defn- get-rotation-anim [object-slide-info object-name]
  (let [object (api.core/get-object-by-name object-name)
        start-rotation (j/get object :rotation)
        end-rotation (:rotation object-slide-info)]
    (cond
      (and (not= object-name :camera)
           start-rotation
           end-rotation
           (not (vector? end-rotation))
           (not (api.core/equals? start-rotation end-rotation)))
      [object-name (api.animation/create-rotation-animation {:start start-rotation
                                                             :end end-rotation
                                                             :duration (:duration object-slide-info)
                                                             :delay (:delay object-slide-info)})]

      (and (not= object-name :camera)
           start-rotation
           end-rotation
           (vector? end-rotation)
           (not (api.core/equals? start-rotation end-rotation)))
      [object-name (api.animation/create-multiple-rotation-animation {:start start-rotation
                                                                      :end end-rotation
                                                                      :duration (:duration object-slide-info)
                                                                      :delay (:delay object-slide-info)})]

      (and (= object-name :camera) (not end-rotation) (not (api.core/equals? start-rotation (j/get object :init-rotation))))
      [object-name (api.animation/create-rotation-animation {:start start-rotation
                                                             :end (api.core/clone (j/get object :init-rotation))
                                                             :duration (:duration object-slide-info)
                                                             :delay (:delay object-slide-info)})]

      (and (= object-name :camera) (vector? end-rotation))
      [object-name (api.animation/create-multiple-rotation-animation {:start start-rotation
                                                                      :end end-rotation
                                                                      :duration (:duration object-slide-info)
                                                                      :delay (:delay object-slide-info)})])))

(defn- get-visibility-anim [object-slide-info object-name]
  (let [end-visibility (:visibility object-slide-info)
        start-visibility (j/get (api.core/get-object-by-name object-name) :visibility)]
    (when (and end-visibility (not= start-visibility end-visibility))
      [object-name (api.animation/create-visibility-animation {:start start-visibility
                                                               :end end-visibility
                                                               :delay (:delay object-slide-info)})])))

(defn- get-alpha-anim [object-slide-info object-name]
  (let [end-alpha (:alpha object-slide-info)
        start-alpha (j/get (api.core/get-object-by-name object-name) :alpha)]
    (when (and end-alpha (not= start-alpha end-alpha))
      [object-name (api.animation/create-alpha-animation {:start start-alpha
                                                          :end end-alpha
                                                          :delay (:delay object-slide-info)})])))

(defn get-target-anim [object-slide-info object-name]
  (when (and object-slide-info (= object-name :camera))
    (let [camera (api.camera/active-camera)
          start (j/call camera :getTarget)
          target (:target object-slide-info)]
      (cond
        (and start target (not (vector? target)) (not (api.core/equals? start target)))
        [object-name (api.animation/create-camera-target-anim {:camera camera
                                                               :target target
                                                               :duration (:duration object-slide-info)
                                                               :delay (:delay object-slide-info)})]

        (and start target (vector? target))
        [object-name (api.animation/create-multiple-target-animation {:camera camera
                                                                      :target target
                                                                      :duration (:duration object-slide-info)
                                                                      :delay (:delay object-slide-info)})]))))

(defn- get-animations-from-slide-info [acc object-slide-info object-name]
  (reduce
    (fn [acc anim-type]
      (let [[object-name anim :as anim-vec] (case anim-type
                                              :position (get-position-anim object-slide-info object-name)
                                              :rotation (get-rotation-anim object-slide-info object-name)
                                              :visibility (get-visibility-anim object-slide-info object-name)
                                              :alpha (get-alpha-anim object-slide-info object-name)
                                              :focus (api.animation/create-focus-camera-anim object-slide-info)
                                              :target (get-target-anim object-slide-info object-name))]
        (cond-> acc
          (and (not= anim-type :focus) anim-vec)
          (conj anim-vec)

          (and (= anim-type :focus) anim-vec)
          (conj [object-name (first anim)] [object-name (second anim)]))))
    acc
    [:position :rotation :visibility :alpha :focus :target]))

(defn- notify-ui [index slides-count]
  (dispatch [::events/update-slide-info index slides-count]))

(defn get-slides []
  (let [slides [{:data {"wave" {:type :wave}
                        "immersa-text" {:type :billboard
                                        :position (v3)
                                        :text "IMMERSA"
                                        :scale 4
                                        :visibility 0}
                        "world" {:type :earth
                                 :position (v3 0 -0.7 -9.5)
                                 :visibility 0}}}
                {:data {"wave" {:type :wave}
                        "immersa-text" {:type :billboard
                                        :visibility 1}
                        "world" {:type :earth
                                 :position (v3 0 -0.7 -8.5)
                                 :visibility 1}
                        "world-cloud-sphere" {:visibility 1}
                        "world-earth-sphere" {:visibility 1}
                        "immersa-text-2" {:type :billboard
                                          :position (v3 0 -0.8 -7.5)
                                          :text "A 3D Presentation Tool for the Web"
                                          :scale 1
                                          :width 3
                                          :height 3
                                          :font-size 35
                                          :visibility 0}}}
                {:data {"wave" {:type :wave}
                        "immersa-text-2" {:type :billboard
                                          :text "A 3D Presentation Tool for the Web"
                                          :visibility 1}
                        "world" {:type :earth
                                 :position (v3 0 0 -7.5)
                                 :visibility 1}
                        "text-3" {:type :text3D
                                  :text "A new dimension to your presentations"
                                  :depth 0.001
                                  :emissive-color api.const/color-white
                                  :size 0.4
                                  :position (v3 0 0 5)
                                  :rotation (v3 (/ js/Math.PI 2) 0 0)
                                  :visibility 0}}}
                {:data {:camera {:position (v3 0 2 -10)}
                        :skybox {:path "img/skybox/space/space"
                                 :speed-factor 0.5}
                        "text-3" {:type :text3D
                                  :position (v3 0 0 -2)
                                  :rotation (v3)
                                  :delay 500
                                  :visibility 1}
                        "image" {:type :image
                                 :path "img/texture/gg.png"
                                 :visibility 0}
                        "world" {:type :earth
                                 :position (v3 0 2.25 -7.5)
                                 :visibility 1}}}

                {:data {:camera {:position (v3 0 2 -1)
                                 :duration 3
                                 :delay 100}
                        :skybox {:path "img/skybox/space/space"}
                        #_#_:skybox {:gradient? true
                                     :speed-factor 1.0}
                        "text-dots" {:type :pcs-text
                                     :text "      Welcome to the\n\n\n\n\n\n\n\nFuture of Presentation"
                                     :visibility 1
                                     :duration 1.5
                                     :point-size 5
                                     :rand-range [-10 20]
                                     :position (v3 -5.5 1 9)
                                     :color api.const/color-white}
                        "particle-cycle" {:type :particle
                                          :duration 2
                                          :position [(v3 -2 0.5 -6)
                                                     (v3 -5 0.5 0)
                                                     (v3 5 0.5 5)
                                                     (v3 0 0 8)]
                                          :target-stop-duration 1.5}
                        "2d-slide-text-1" {:type :text3D
                                           :text "From 2D clarity to..."
                                           :depth 0.001
                                           :emissive-color api.const/color-white
                                           :size 0.215
                                           :position (v3 0 2.65 5)
                                           :visibility 0}
                        "2d-slide-text-2" {:type :text3D
                                           :text "3D IMMERSION"
                                           :depth 0.1
                                           :size 0.35
                                           :position (v3 0 1.53 9.1)
                                           :hl-color [1 1 1]
                                           :visibility 0}
                        "2d-slide" {:type :image
                                    :path "img/texture/2d-slide.png"
                                    :scale 3.7
                                    :position (v3 0 1 9)
                                    :rotation (v3)
                                    :visibility 0}
                        "box" {:type :box
                               :position (v3 0 2 0)
                               :rotation (v3 1.2 2.3 4.1)
                               :visibility 0}}}

                {:data {:camera {:position (v3 0 2 -1)}
                        :skybox {:path "img/skybox/space/space"}
                        "2d-slide" {:visibility 1}
                        "2d-slide-text-1" {:visibility 1}
                        "2d-slide-text-2" {:visibility 0}
                        "plane" {:type :glb
                                 :path "model/plane.glb"
                                 :position (v3 0 -1 50)
                                 :rotation (v3 0 Math/PI 0)}

                        "3d-slide-text-1" {:type :text3D
                                           :text "$2.3B\n(TOM)"
                                           :depth 0.1
                                           :size 0.25
                                           :position (v3 -1.5 1.5 5)
                                           :hl-color [0.99 0.8 1]
                                           :visibility 0}
                        "3d-slide-text-2" {:type :text3D
                                           :text "$242.9M\n  (SAM)"
                                           :depth 0.1
                                           :size 0.25
                                           :position (v3 0 1.5 5)
                                           :hl-color [0.9 0.8 0.4]
                                           :visibility 0}
                        "3d-slide-text-3" {:type :text3D
                                           :text "$15M\n(SOM)"
                                           :depth 0.1
                                           :size 0.25
                                           :position (v3 1.5 1.5 5)
                                           :hl-color [0.9 0.88 0.88]
                                           :visibility 0}}}

                {:data {:camera {:position (v3 0 2 -1)
                                 :rotation [(v3 (/ Math/PI -7) 0 0)
                                            (v3)]
                                 :duration 3.5
                                 :delay 1250}
                        :skybox {:path "img/skybox/space/space"}
                        ;; "2d-slide-text-1" {}
                        "2d-slide-text-2" {:position (v3 0 2.75 5)
                                           :visibility 1}
                        "2d-slide" {:visibility 0
                                    :rotation (v3 (/ js/Math.PI 2) 0 0)}
                        "3d-slide-text-1" {:visibility 1
                                           :delay 3000}
                        "3d-slide-text-2" {:visibility 1
                                           :delay 3000}
                        "3d-slide-text-3" {:visibility 1
                                           :delay 3000}
                        "plane" {:type :glb
                                 :position (v3 0 5 -10)
                                 :rotation (v3 -0.25 Math/PI 0.15)
                                 :delay 750
                                 :duration 4}}}

                {:data {:camera {:position (v3 0 2 -1)}
                        :skybox {:path "img/skybox/space/space"}}}

                {:data {:camera {:focus "box"
                                 :type :right}
                        "billboard-2" {:type :billboard
                                       :position (v3 -3 2.3 0)
                                       :text "❖ Ready-Made Templates\n\n❖ High-Performance Render\n\n❖ User-Friendly UI/UX"
                                       :scale 2
                                       :font-weight "bold"
                                       :visibility 1}}}

                {:data {:camera {:position (v3 0 0 -10)
                                 :rotation (v3 0 0 0)}}}

                {:data {"enjoy-text" {:type :billboard
                                      :text "✦ Enjoy the Immersive Experience ✦"
                                      :scale 5
                                      :width 3
                                      :height 3
                                      :font-size 30
                                      :visibility 1}}}

                {:data {:camera {:position (v3 0 0 50)}
                        "enjoy-text" {:visibility 0}}}]
        slides-vec (vec (map-indexed #(assoc %2 :index %1) slides))
        props-to-copy [:type :position :rotation :visibility]
        clone-if-exists (fn [data]
                          (let [position (:position data)
                                rotation (:rotation data)]
                            (cond-> data
                              (vector? position)
                              (assoc :position (mapv api.core/clone (:position data)))

                              (and position (not (vector? position)))
                              (assoc :position (api.core/clone (:position data)))

                              (vector? rotation)
                              (assoc :rotation (mapv api.core/clone (:rotation data)))

                              (and rotation (not (vector? rotation)))
                              (assoc :rotation (api.core/clone (:rotation data))))))]
    (reduce
      (fn [slides-vec slide]
        (let [prev-slide-data (get-in slides-vec [(dec (:index slide)) :data])
              slide-data (:data slide)]
          (conj slides-vec
                (assoc slide :data
                       (reduce-kv
                         (fn [acc name objet-slide-data]
                           (if-let [prev-slide-data (get prev-slide-data name)]
                             (assoc acc name (merge (clone-if-exists (select-keys prev-slide-data props-to-copy)) objet-slide-data))
                             (assoc acc name objet-slide-data)))
                         {}
                         slide-data)))))
      [(first slides-vec)]
      (rest slides-vec))))

(defn- run-skybox-dissolve-animation [objects-data]
  (let [skybox-path (-> objects-data :skybox :path)
        speed-factor (or (-> objects-data :skybox :speed-factor) 0.5)
        skybox-shader (api.core/get-object-by-name "skybox-shader")
        current-skybox-path (j/get skybox-shader :skybox-path)
        default-skybox-path (j/get skybox-shader :default-skybox-path)
        new-skybox-path (cond
                          (and skybox-path (not= skybox-path current-skybox-path))
                          skybox-path

                          (and (nil? skybox-path) (not= default-skybox-path current-skybox-path))
                          default-skybox-path)]
    (when new-skybox-path
      (api.animation/create-skybox-dissolve-anim
        :skybox-path new-skybox-path
        :speed-factor speed-factor))))

(defmulti disable-component (comp keyword api.core/get-object-type-by-name))

(defn- disable-mesh-component-via-visibility [name]
  (let [mesh (api.core/get-object-by-name name)
        visibility (j/get mesh :visibility)
        duration 0.5
        to (* 30 1.0)]
    (api.animation/begin-direct-animation
      :target mesh
      :animations (api.animation/create-visibility-animation {:start visibility
                                                              :end 0
                                                              :duration duration})
      :to to
      :on-animation-end #(api.core/set-enabled mesh false))))

(defmethod disable-component :text [name]
  (let [text (api.core/get-object-by-name name)
        alpha (j/get text :alpha)
        duration 0.5
        to (* 30 1.0)]
    (api.animation/begin-direct-animation
      :target text
      :animations (api.animation/create-alpha-animation {:start alpha
                                                         :end 0
                                                         :duration duration})
      :to to)))

(defmethod disable-component :earth [name]
  (let [earth-sphere (api.core/get-object-by-name (str name "-earth-sphere"))
        cloud-sphere (api.core/get-object-by-name (str name "-cloud-sphere"))
        hl (j/get-in api.core/db [:nodes name :hl])
        earth-sphere-visibility (j/get earth-sphere :visibility)
        cloud-sphere-visibility (j/get cloud-sphere :visibility)
        duration 0.5
        to (* 30 duration)]
    (j/call hl :removeMesh cloud-sphere)
    (api.animation/begin-direct-animation
      :target earth-sphere
      :animations (api.animation/create-visibility-animation {:start earth-sphere-visibility
                                                              :end 0
                                                              :duration duration})
      :to to
      :on-animation-end #(api.core/set-enabled earth-sphere false))
    (api.animation/begin-direct-animation
      :target cloud-sphere
      :animations (api.animation/create-visibility-animation {:start cloud-sphere-visibility
                                                              :end 0
                                                              :duration duration})
      :to to
      :on-animation-end #(api.core/set-enabled cloud-sphere false))))

(defmethod disable-component :box [name]
  (disable-mesh-component-via-visibility name))

(defmethod disable-component :billboard [name]
  (disable-mesh-component-via-visibility name))

(defmethod disable-component :image [name]
  (disable-mesh-component-via-visibility name))

(defmethod disable-component :text3D [name]
  (disable-mesh-component-via-visibility name))

(defmethod disable-component :glb [name]
  (api.core/set-enabled (api.core/get-object-by-name name) false))

(defmethod disable-component :pcs-text [name]
  (let [mesh (api.core/get-object-by-name name)
        pcs (j/get mesh :pcs)
        visibility (j/get mesh :visibility)
        duration 0.5
        to (* 30 duration)]
    (api.animation/begin-direct-animation
      :target mesh
      :animations (api.animation/create-visibility-animation {:start visibility
                                                              :end 0
                                                              :duration duration})
      :to to
      :on-animation-end (fn []
                          (api.core/dispose mesh)
                          (api.core/dispose pcs)))))

(defmethod disable-component :wave [name]
  (let [pcs (api.core/get-object-by-name name)
        mesh (j/get pcs :mesh)
        visibility (j/get mesh :visibility)
        duration 0.5
        to (* 30 duration)]
    (api.animation/begin-direct-animation
      :target mesh
      :animations (api.animation/create-visibility-animation {:start visibility
                                                              :end 0
                                                              :duration duration})
      :to to
      :on-animation-end (fn []
                          (api.core/dispose mesh)
                          (api.core/dispose name)))))

(defmethod disable-component :particle [name]
  (let [ps (api.core/get-object-by-name name)]
    (api.particle/stop ps)
    (api.particle/reset ps)))

(defmethod disable-component :default [name]
  (println "dispose-component Default: " name))

(defmulti enable-component (fn [name _]
                             (-> name api.core/get-object-type-by-name keyword)))

(defn- enable-mesh-component [name]
  (api.core/set-enabled (api.core/get-object-by-name name) true))

(defmethod enable-component :earth [name slide-info]
  (let [hl (j/get-in api.core/db [:nodes name :hl])
        earth-sphere (api.core/get-object-by-name (str name "-earth-sphere"))
        cloud-sphere (api.core/get-object-by-name (str name "-cloud-sphere"))
        duration 0.5
        to (* 30 duration)]
    (api.core/set-enabled earth-sphere true)
    (api.core/set-enabled cloud-sphere true)
    (j/call hl :addMesh cloud-sphere (api.core/color 0.3 0.74 0.94))
    (api.animation/begin-direct-animation
      :target earth-sphere
      :animations (api.animation/create-visibility-animation {:start (j/get earth-sphere :visibility)
                                                              :end (:visibility slide-info)
                                                              :duration duration})
      :to to)
    (api.animation/begin-direct-animation
      :target cloud-sphere
      :animations (api.animation/create-visibility-animation {:start (j/get cloud-sphere :visibility)
                                                              :end (:visibility slide-info)
                                                              :duration duration})
      :to to)))

(defmethod enable-component :box [name _]
  (enable-mesh-component name))

(defmethod enable-component :billboard [name _]
  (enable-mesh-component name))

(defmethod enable-component :image [name _]
  (enable-mesh-component name))

(defmethod enable-component :text3D [name _]
  (enable-mesh-component name))

(defmethod enable-component :glb [name _]
  (enable-mesh-component name))

(defmethod enable-component :wave [name _]
  (when-not (api.core/get-object-by-name name)
    (api.component/wave name)))

(defmethod enable-component :particle [name _]
  (api.particle/start (api.core/get-object-by-name name)))

(defmethod enable-component :default [name]
  (println "enable-component Default: " name))

(defn start-slide-show []
  (let [command-ch (a/chan (a/dropping-buffer 1))
        slide-controls (js/document.getElementById "slide-controls")
        prev-button (j/get-in slide-controls [:children 0])
        next-button (j/get-in slide-controls [:children 2])]
    (a/put! command-ch :next)
    (api.core/dispose-all (concat (api.core/get-objects-by-type "box")
                                  (api.core/get-objects-by-type "billboard")
                                  (api.core/get-objects-by-type "text")
                                  (api.core/get-objects-by-type "earth")
                                  (api.core/get-objects-by-type "wave")))
    (api.camera/reset-camera)
    (api.camera/detach-control (api.camera/active-camera))
    (common.utils/remove-element-listeners)
    (common.utils/register-event-listener prev-button "click"
                                          (fn [e]
                                            (when-not (j/get e :repeat)
                                              (a/put! command-ch :prev))))
    (common.utils/register-event-listener next-button "click"
                                          (fn [e]
                                            (when-not (j/get e :repeat)
                                              (a/put! command-ch :next))))
    (common.utils/register-event-listener js/window "keydown"
                                          (fn [e]
                                            (when-not (j/get e :repeat)
                                              (cond
                                                (or (= (.-keyCode e) 39)
                                                    (= (.-keyCode e) 40)) (a/put! command-ch :next)
                                                (or (= (.-keyCode e) 37)
                                                    (= (.-keyCode e) 38)) (a/put! command-ch :prev)))))
    (go-loop [index -1]
      (let [command (a/<! command-ch)
            current-index (case command
                            :next (inc index)
                            :prev (dec index))
            slides (get-slides)]
        ;; (cljs.pprint/pprint slides)
        (if (and (>= current-index 0) (< current-index (count slides)))
          (let [_ (notify-ui current-index (count slides))
                slide (slides current-index)
                objects-data (:data slide)
                object-names-from-slide-info (set (conj (keys (:data slide)) :camera))
                _ (when (object-names-from-slide-info :camera)
                    (api.camera/update-active-camera))
                object-names-from-objects (-> slide :objects keys)
                objects-to-create (filter #(not (api.core/get-object-by-name %)) object-names-from-slide-info)
                current-slide-object-names (-> slide :data keys set)
                [prev-slide-object-names object-names-to-dispose] (when (> current-index 0)
                                                                    (let [prev-slide (slides (if (= command :next)
                                                                                               (dec current-index)
                                                                                               (inc current-index)))
                                                                          prev-slide-object-names (-> prev-slide :data keys set)]
                                                                      [prev-slide-object-names
                                                                       (set/difference prev-slide-object-names current-slide-object-names #{:camera :skybox})]))

                _ (doseq [name object-names-to-dispose]
                    (disable-component name))
                _ (doseq [name objects-to-create]
                    (let [params (get objects-data name)
                          type (:type params)
                          params (dissoc params :type)]
                      (case type
                        :glb (api.mesh/glb->mesh name params)
                        :wave (api.component/wave name)
                        :box (api.component/create-box-with-numbers name params)
                        :earth (api.component/earth name params)
                        :text3D (api.mesh/text name params)
                        :text (api.gui/add-control
                                (api.core/get-advanced-texture)
                                (api.gui/text-block name params))
                        :image (api.component/image name params)
                        :particle (api.particle/sparkles name params)
                        :billboard (api.component/billboard name params)
                        nil)))
                animations (reduce
                             (fn [acc object-name]
                               (let [object-slide-info (get-in slide [:data object-name])]
                                 (get-animations-from-slide-info acc object-slide-info object-name)))
                             []
                             object-names-from-slide-info)
                animations-data (vals
                                  (reduce-kv
                                    (fn [acc name animations]
                                      (let [animations (mapv second animations)
                                            delay (first (keep (j/get :delay) animations))
                                            duration (first (keep (j/get :duration) animations))
                                            max-fps (apply max (map (j/get :framePerSecond) animations))
                                            target (api.core/get-object-by-name name)]
                                        (if target
                                          (assoc acc name {:target (api.core/get-object-by-name name)
                                                           :animations animations
                                                           :delay delay
                                                           :from 0
                                                           :to (* max-fps duration)})
                                          acc)))
                                    {}
                                    (group-by first animations)))
                prev-and-gradient? (and (= :prev command)
                                        (-> (:data (slides (inc current-index))) :skybox :gradient?))
                skybox-dissolve-anim (when-not prev-and-gradient?
                                       (run-skybox-dissolve-animation objects-data))
                _ (doseq [name (set/difference current-slide-object-names
                                               object-names-to-dispose
                                               (set prev-slide-object-names))]
                    (enable-component name (get-in slide [:data name])))
                channels (mapv #(api.animation/begin-direct-animation %) animations-data)
                pcs-animations (keep
                                 (fn [object-name]
                                   (let [object-slide-info (get-in slide [:data object-name])]
                                     (when (and (#{:pcs-text} (:type object-slide-info)))
                                       (api.animation/pcs-text-anim object-name object-slide-info))))
                                 object-names-from-slide-info)]
            (some-> skybox-dissolve-anim a/<!)
            (cond
              (-> objects-data :skybox :gradient?)
              (a/<! (api.animation/create-sky-sphere-dissolve-anim))

              prev-and-gradient?
              (do
                (a/<! (api.animation/create-reverse-sky-sphere-dissolve-anim))
                (some-> (run-skybox-dissolve-animation objects-data) a/<!)))

            (doseq [c channels]
              (a/<! c))

            (doseq [c pcs-animations]
              (a/<! c))

            (recur current-index))
          (recur index))))))
