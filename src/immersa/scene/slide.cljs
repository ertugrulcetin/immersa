(ns immersa.scene.slide
  (:require
    [applied-science.js-interop :as j]
    [cljs.core.async :as a :refer [go go-loop <!]]
    [clojure.set :as set]
    [clojure.walk :as walk]
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
    [immersa.scene.materials-in-sphere :as mat.spheres]
    [re-frame.core :refer [dispatch]]))

(defn- get-position-anim [object-slide-info object-name]
  (let [object (api.core/get-object-by-name object-name)
        start-position (j/get object :position)
        end-position (:position object-slide-info)
        camera? (= object-name :camera)]
    (let [end-position (cond
                         (and start-position
                              end-position
                              (or (and
                                    (not (vector? end-position))
                                    (not (api.core/equals? start-position end-position)))
                                  (and (vector? end-position)
                                       (> (count end-position) 1))))
                         end-position

                         (and camera?
                              start-position
                              (not end-position)
                              (not (api.core/equals? start-position (j/get object :init-position))))
                         (api.core/clone (j/get object :init-position)))]
      (when end-position
        [object-name (api.animation/create-position-animation (assoc object-slide-info
                                                                     :start start-position
                                                                     :end end-position))]))))

(defn- get-rotation-anim [object-slide-info object-name]
  (let [object (api.core/get-object-by-name object-name)
        start-rotation (j/get object :rotation)
        end-rotation (:rotation object-slide-info)
        camera? (= object-name :camera)]
    (let [end-rotation (cond
                         (and end-rotation
                              (or (and
                                    (not (vector? end-rotation))
                                    (not (api.core/equals? start-rotation end-rotation)))
                                  (and (vector? end-rotation)
                                       (> (count end-rotation) 1))))
                         end-rotation

                         (and camera?
                              (not end-rotation)
                              (not (api.core/equals? start-rotation (j/get object :init-rotation))))
                         (api.core/clone (j/get object :init-rotation)))]
      (when end-rotation
        [object-name (api.animation/create-rotation-animation (assoc object-slide-info
                                                                     :start start-rotation
                                                                     :end end-rotation))]))))

(defn- get-scale-anim [object-slide-info object-name]
  (let [object (api.core/get-object-by-name object-name)
        start-scale (j/get object :scaling)
        end-scale (:scale object-slide-info)
        camera? (= object-name :camera)]
    (when-not camera?
      (let [end-scale (when
                        (and start-scale
                             end-scale
                             (or (and
                                   (not (vector? end-scale))
                                   (not (api.core/equals? start-scale end-scale)))
                                 (and (vector? end-scale)
                                      (> (count end-scale) 1))))
                        end-scale)]
        (when end-scale
          [object-name (api.animation/create-scale-animation (assoc object-slide-info
                                                                    :start start-scale
                                                                    :end end-scale))])))))

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
          start-target (j/call camera :getTarget)
          end-target (:target object-slide-info)
          end-target (and end-target
                          (or (and
                                (not (vector? end-target))
                                (not (api.core/equals? start-target end-target)))
                              (and (vector? end-target)
                                   (> (count end-target) 1))))]
      (when end-target
        [object-name (api.animation/create-camera-target-anim {:camera camera
                                                               :target end-target
                                                               :duration (:duration object-slide-info)
                                                               :delay (:delay object-slide-info)})]))))

(defn- get-animations-from-slide-info [acc object-slide-info object-name]
  (reduce
    (fn [acc anim-type]
      (let [[object-name anim :as anim-vec] (case anim-type
                                              :position (get-position-anim object-slide-info object-name)
                                              :rotation (get-rotation-anim object-slide-info object-name)
                                              :scale (get-scale-anim object-slide-info object-name)
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
    [:position :rotation :scale :visibility :alpha :focus :target]))

(defn- notify-ui [index slides-count]
  (dispatch [::events/update-slide-info index slides-count]))

(defn- parse-pos-rot-scale [type form]
  (cond
    (and (map? form)
         (type form)
         (number? (type form)))
    (assoc form type (v3 (type form)))

    (and (map? form)
         (type form)
         (vector? (type form))
         (number? (first (type form))))
    (assoc form type (apply v3 (type form)))

    (and (map? form)
         (type form)
         (vector? (type form))
         (vector? (first (type form))))
    (assoc form type (mapv (partial apply v3) (type form)))

    :else form))

(defn- parse-colors [type form]
  (let [color-kw->color (fn [kw]
                          (case kw
                            :color/white (api.const/color-white)
                            :color/black (api.const/color-black)
                            :color/red (api.const/color-red)
                            :color/yellow (api.const/color-yellow)
                            :color/teal (api.const/color-teal)
                            :color/gray (api.const/color-gray)))]
    (cond
      (and (map? form)
           (type form)
           (keyword? (type form)))
      (assoc form type (color-kw->color (type form)))

      (and (map? form)
           (type form)
           (vector? (type form)))
      (assoc form type (apply api.core/color-rgb (type form)))

      (and (map? form)
           (type form)
           (number? (type form)))
      (assoc form type (api.core/color-rgb (type form) (type form) (type form)))

      :else form)))

(defn- parse-slides [slides]
  (walk/prewalk
    (fn [form]
      (->> form
           (parse-pos-rot-scale :position)
           (parse-pos-rot-scale :rotation)
           (parse-pos-rot-scale :scale)
           (parse-colors :color)
           (parse-colors :hl-color)
           (parse-colors :albedo-color)
           (parse-colors :emissive-color)))
    slides))

;; TODO is is called every time a slide is changed
(defn get-slides []
  (let [slides [{:data {:skybox {:background {:color 103}}
                        "wave" {:type :wave}
                        "immersa-text" {:type :billboard
                                        :position 0
                                        :text "IMMERSA"
                                        :scale 4
                                        :visibility 0}
                        "world" {:type :earth
                                 :position [0 -0.7 -9.5]
                                 :visibility 0}}}
                {:data {:skybox {:background {:color 0}}
                        "wave" {:type :wave}
                        "immersa-text" {:type :billboard
                                        :visibility 1}
                        "world" {:type :earth
                                 :position [0 -0.7 -8.5]
                                 :visibility 1}
                        "world-cloud-sphere" {:visibility 1}
                        "world-earth-sphere" {:visibility 1}
                        "immersa-text-2" {:type :billboard
                                          :position [0 -0.8 -7.5]
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
                                 :position [0 0 -7.5]
                                 :visibility 1}
                        "text-3" {:type :text3D
                                  :text "A new dimension to your presentations"
                                  :depth 0.001
                                  :emissive-color :color/white
                                  :size 0.4
                                  :position [0 0 5]
                                  :rotation [(/ js/Math.PI 2) 0 0]
                                  :visibility 0}}}
                {:data {:camera {:position [0 2 -10]}
                        :skybox {:path "img/skybox/space/space"
                                 :speed-factor 0.5}
                        "text-3" {:type :text3D
                                  :position [0 0 -2]
                                  :rotation 0
                                  :delay 500
                                  :visibility 1}
                        "image" {:type :image
                                 :path "img/texture/gg.png"
                                 :visibility 0}
                        "world" {:type :earth
                                 :position [0 2.25 -7.5]
                                 :visibility 1}}}

                {:data {:camera {:position [0 2 -1]
                                 :duration 3
                                 :delay 100}
                        :skybox {:path "img/skybox/space/space"}
                        "text-dots" {:type :pcs-text
                                     :text "      Welcome to the\n\n\n\n\n\n\n\nFuture of Presentation"
                                     :duration 1.5
                                     :delay 2500
                                     :point-size 5
                                     :rand-range [-10 20]
                                     :position [-5.5 1 9]
                                     :color :color/white}
                        "particle-cycle" {:type :particle
                                          :particle-type :sparkle
                                          :duration 2
                                          :position [[-2 0.5 -6]
                                                     [-5 0.5 0]
                                                     [5 0.5 5]
                                                     [0 0 8]]
                                          :target-stop-duration 1.5}
                        "2d-slide-text-1" {:type :text3D
                                           :text "From 2D Clarity to..."
                                           :depth 0.001
                                           :emissive-color :color/white
                                           :size 0.215
                                           :position [0 2.65 5]
                                           :visibility 0}
                        "2d-slide-text-2" {:type :text3D
                                           :text "3D IMMERSION"
                                           :nme :purple-glass
                                           :depth 0.1
                                           :size 0.35
                                           :position [0 1.53 9.1]
                                           :visibility 0}
                        "2d-slide" {:type :image
                                    :path "img/texture/2d-slide.png"
                                    :scale 2
                                    :position [0 1 9]
                                    :rotation (v3)
                                    :visibility 0}
                        "box" {:type :box
                               :position [0 2 0]
                               :rotation [1.2 2.3 4.1]
                               :visibility 0}}}

                {:data {:camera {:position [0 2 -1]}
                        :skybox {:path "img/skybox/space/space"}
                        "2d-slide" {:visibility 1
                                    :scale 3.7}
                        "2d-slide-text-1" {:visibility 1}
                        "2d-slide-text-2" {:visibility 0}
                        "plane" {:type :glb
                                 :path "model/plane_2.glb"
                                 :position [0 -1 50]
                                 :rotation [0 Math/PI 0]}

                        "3d-slide-text-1" {:type :text3D
                                           :text "$412B\n(TOM)"
                                           :depth 0.1
                                           :size 0.25
                                           :position [-1.5 1.5 5]
                                           :hl-color [252.45 204 255]
                                           :hl-blur 0.5
                                           :visibility 0}
                        "3d-slide-text-2" {:type :text3D
                                           :text "$177.2B\n  (SAM)"
                                           :depth 0.1
                                           :size 0.25
                                           :position [0 1.5 5]
                                           :hl-color [229.5 204 102]
                                           :hl-blur 0.5
                                           :visibility 0}
                        "3d-slide-text-3" {:type :text3D
                                           :text "$1.78B\n (SOM)"
                                           :depth 0.1
                                           :size 0.25
                                           :position [1.5 1.5 5]
                                           :hl-color [229.5 224.4 224.4]
                                           :hl-blur 0.5
                                           :visibility 0}}}

                {:data {:camera {:position [0 2 -1]
                                 :rotation [[(/ Math/PI -7) 0 0]
                                            [0 0 0]]
                                 :duration 3.5
                                 :delay 1250}
                        :skybox {:path "img/skybox/space/space"}
                        ;; "2d-slide-text-1" {}
                        "2d-slide-text-2" {:position [0 2.75 5]
                                           :visibility 1}
                        "2d-slide" {:visibility 0
                                    :rotation [(/ js/Math.PI 2) 0 0]}
                        "3d-slide-text-1" {:visibility 1
                                           :delay 3000}
                        "3d-slide-text-2" {:visibility 1
                                           :delay 3000}
                        "3d-slide-text-3" {:visibility 1
                                           :delay 3000}
                        "plane" {:type :glb
                                 :position [0 5 -10]
                                 :rotation [-0.25 Math/PI 0.15]
                                 :delay 750
                                 :duration 4}
                        "porche" {:type :glb
                                  :path "model/porche_911.glb"
                                  :position [-15 1 5]
                                  :rotation [0 (/ Math/PI 8) 0]
                                  :update-materials {"paint" {:albedo-color :color/black}}}
                        "3d-lib-header" {:type :text3D
                                         :text "Extensive 3D Library"
                                         :depth 0.001
                                         :emissive-color :color/white
                                         :size 0.7
                                         :position [15 4 9]
                                         :visibility 1}
                        "3d-lib-text-2" {:type :text3D
                                         :text "Over 100 pre-built 3D models at your fingertips"
                                         :depth 0.001
                                         :emissive-color :color/white
                                         :size 0.3
                                         :position [19 3 9]
                                         :visibility 1}
                        "3d-lib-text-3" {:type :text3D
                                         :text "ready to elevate your presentations"
                                         :depth 0.001
                                         :emissive-color :color/white
                                         :size 0.3
                                         :position [19 2.5 9]
                                         :visibility 1}
                        "cloud-particle" {:type :particle
                                          :particle-type :cloud
                                          :position [10 2.1 2]
                                          :scale 0.9
                                          :update-speed 0.01}}}

                {:data {:camera {:position [0 2 -1]
                                 :rotation 0}
                        ;; TODO replace color and parse here as well
                        :skybox {:background {:color 103}
                                 :speed-factor 1.0}
                        "porche" {:type :glb
                                  :position [-1.25 1 5]}

                        "3d-lib-header" {:position [0 4 9]
                                         :duration 1.5
                                         :visibility 1}
                        "3d-lib-text-2" {:position [0 3 9]
                                         :duration 1.5
                                         :delay 250
                                         :visibility 1}
                        "3d-lib-text-3" {:position [0 2.5 9]
                                         :duration 1.5
                                         :delay 350
                                         :visibility 1}
                        "cloud-particle" {:delay 600
                                          :position [0 2.1 2]}
                        "3d-lib-header-2" {:type :text3D
                                           :text "Personalized Imports"
                                           :depth 0.001
                                           :emissive-color :color/white
                                           :size 0.55
                                           :position [15 4 9]
                                           :visibility 1}
                        "3d-lib-text-4" {:type :text3D
                                         :text "Seamlessly integrate your own models"
                                         :depth 0.001
                                         :emissive-color :color/white
                                         :size 0.25
                                         :position [19 3 9]
                                         :visibility 1}
                        "3d-lib-text-5" {:type :text3D
                                         :text "making each presentation uniquely yours"
                                         :depth 0.001
                                         :emissive-color :color/white
                                         :size 0.25
                                         :position [19 2.5 9]
                                         :visibility 1}}}

                {:data {:camera {:position [0 1 -1]}
                        :skybox {:background {:color 103}
                                 :speed-factor 1.0}
                        "porche" {:type :glb
                                  :path "model/porche_911.glb"
                                  :position [-0.7 1 3]
                                  :rotation [0 Math/PI 0]}
                        "3d-lib-header-2" {:type :text3D
                                           :position [0 4 9]
                                           :duration 1.5
                                           :visibility 1}
                        "3d-lib-text-4" {:position [0 3 9]
                                         :duration 1.5
                                         :delay 250
                                         :visibility 1}
                        "3d-lib-text-5" {:position [3.5 2.5 9]
                                         :duration 1.5
                                         :delay 350
                                         :visibility 1}
                        "cloud-particle" {:position [1 1.3 2]}
                        "sphere-text" {:type :text3D
                                       :text "Rich Material Library"
                                       :depth 0.001
                                       :emissive-color :color/white
                                       :size 0.25
                                       :position [0 5.5 4]
                                       :visibility 1}
                        "sphere1" {:type :sphere-mat
                                   :component :specular
                                   :position [0 -2 4]}
                        "sphere2" {:type :sphere-mat
                                   :component :stone
                                   :position [4.5 0.75 4]}
                        "sphere3" {:type :sphere-mat
                                   :component :copper
                                   :position [-4.5 0.75 4]}
                        "sphere4" {:type :sphere-mat
                                   :component :nebula
                                   :position [0 5.5 4]}
                        "sphere5" {:type :sphere-mat
                                   :component :world
                                   :position [4.5 2.25 4]}
                        "sphere6" {:type :sphere-mat
                                   :component :translucent
                                   :position [-4.5 2.25 4]}}}

                {:data {:camera {:position [0 2 -1]
                                 :rotation 0}
                        :skybox {:background {:color [80 157 105]}
                                 :speed-factor 1.0}
                        "porche" {:type :glb
                                  :path "model/porche_911.glb"
                                  :position [-0.7 1 500]}
                        "sphere-text" {:position [0 3.3 4]}
                        "sphere1" {:position [0 0.75 4]}
                        "sphere2" {:position [2 0.75 4]}
                        "sphere3" {:position [-2 0.75 4]}
                        "sphere4" {:position [0 2.25 4]}
                        "sphere5" {:position [2 2.25 4]}
                        "sphere6" {:position [-2 2.25 4]}
                        "sphere-text-2" {:type :greased-line
                                         :text "Over 30 ready-made materials"
                                         :color :color/white
                                         :position [-0.5 2 5]
                                         :rotation [(/ Math/PI 2) 0 0]
                                         :width 0.01
                                         :size 0.27
                                         :visibility 0}
                        "sphere-text-3" {:type :greased-line
                                         :text "for enhancing texts and models"
                                         :color :color/white
                                         :position [-0.25 1.6 5]
                                         :rotation [(/ Math/PI 2) 0 0]
                                         :width 0.01
                                         :size 0.23
                                         :visibility 0}}}

                {:data {:camera {:position [0 2 -1.5]
                                 :rotation 0}
                        :skybox {:background {:color [80 157 105]}
                                 :speed-factor 1.0}
                        "sphere-text-2" {:position [-0.5 2 5]
                                         :rotation 0
                                         :visibility 1}
                        "sphere-text-3" {:position [-0.25 1.6 5]
                                         :rotation 0
                                         :visibility 1}
                        "sphere1" {:position [-2.5 0.75 4]}
                        "sphere2" {:position [-1.2 0.75 4]}
                        "sphere3" {:position [-2.5 2 4]}
                        "sphere4" {:position [-1.2 2 4]}
                        "sphere5" {:position [-1.2 3.25 4]}
                        "sphere6" {:position [-2.5 3.25 4]}
                        "enjoy-text" {:type :billboard
                                      :text "✦ Enjoy the Immersive Experience ✦"
                                      :scale 5
                                      :width 3
                                      :height 3
                                      :font-size 30
                                      :visibility 0}
                        "join-text" {:type :billboard
                                     :position [0 -0.75 0]
                                     :text "Join Waitlist"
                                     :scale 5
                                     :width 3
                                     :height 3
                                     :font-size 30
                                     :visibility 0}}}

                {:data {:camera {:position [0 0 -10]
                                 :rotation 0}
                        :skybox {:gradient? true
                                 :speed-factor 1.0}
                        "enjoy-text" {:type :billboard
                                      :visibility 1
                                      :delay 600}
                        "join-text" {:type :billboard
                                     :visibility 1
                                     :delay 900}}}]
        slides (parse-slides slides)
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

(defmethod disable-component :greased-line [name]
  (disable-mesh-component-via-visibility name))

(defmethod disable-component :glb [name]
  (api.core/set-enabled (api.core/get-object-by-name name) false))

(defmethod disable-component :sphere-mat [name]
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

(defmethod enable-component :greased-line [name _]
  (enable-mesh-component name))

(defmethod enable-component :glb [name _]
  (enable-mesh-component name))

(defmethod enable-component :sphere-mat [name _]
  (enable-mesh-component name))

(defmethod enable-component :wave [name _]
  (when-not (api.core/get-object-by-name name)
    (api.component/wave name)))

(defmethod enable-component :particle [name _]
  (api.particle/start (api.core/get-object-by-name name)))

(defmethod enable-component :default [name]
  (println "enable-component Default: " name))

(let [circuit-breaker-running? (atom false)]
  (defn process-next-prev-command [type ch slide-in-progress? current-running-anims]

    (if (and @slide-in-progress? (not @circuit-breaker-running?))
      (do
        (reset! circuit-breaker-running? true)
        (println "Circuit breaker tripped")
        (doseq [{:keys [force-finish-fn force-finish-fn-atom]} @current-running-anims]
          (if-let [force-finish-fn (some-> force-finish-fn-atom deref)]
            (force-finish-fn)
            (force-finish-fn)))
        (reset! current-running-anims [])
        (reset! circuit-breaker-running? false))
      (a/put! ch type))))

(defn start-slide-show []
  (let [command-ch (a/chan (a/dropping-buffer 1))
        slide-controls (js/document.getElementById "slide-controls")
        prev-button (j/get-in slide-controls [:children 0])
        next-button (j/get-in slide-controls [:children 2])
        current-running-anims (atom [])
        slide-in-progress? (atom false)]
    (a/put! command-ch :next)
    (api.core/dispose-all (concat (api.core/get-objects-by-type "box")
                                  (api.core/get-objects-by-type "billboard")
                                  (api.core/get-objects-by-type "text")
                                  (api.core/get-objects-by-type "earth")
                                  (api.core/get-objects-by-type "wave")))
    (api.camera/reset-camera)
    (api.camera/detach-control (api.camera/active-camera))
    (common.utils/register-event-listener prev-button "click"
                                          (fn [e]
                                            (when-not (j/get e :repeat)
                                              (process-next-prev-command :prev command-ch slide-in-progress? current-running-anims))))
    (common.utils/register-event-listener next-button "click"
                                          (fn [e]
                                            (when-not (j/get e :repeat)
                                              (process-next-prev-command :next command-ch slide-in-progress? current-running-anims))))
    (common.utils/register-event-listener js/window "keydown"
                                          (fn [e]
                                            (when-not (j/get e :repeat)
                                              (cond
                                                (or (= (.-keyCode e) 39)
                                                    (= (.-keyCode e) 40))
                                                (process-next-prev-command :next command-ch slide-in-progress? current-running-anims)
                                                (or (= (.-keyCode e) 37)
                                                    (= (.-keyCode e) 38))
                                                (process-next-prev-command :prev command-ch slide-in-progress? current-running-anims)))))
    (go-loop [index -1]
      (let [command (a/<! command-ch)
            current-index (case command
                            :next (inc index)
                            :prev (dec index))
            slides (get-slides)]
        (if (and (>= current-index 0) (< current-index (count slides)))
          (let [_ (notify-ui current-index (count slides))
                slide (slides current-index)
                objects-data (:data slide)
                object-names-from-slide-info (set (conj (keys (:data slide)) :camera))
                _ (when (object-names-from-slide-info :camera)
                    (api.camera/update-active-camera))
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
                        :particle (case (:particle-type params)
                                    :sparkle (api.particle/sparkles name params)
                                    :cloud (api.particle/clouds name params))
                        :billboard (api.component/billboard name params)
                        :sphere-mat (mat.spheres/get-sphere name params)
                        :greased-line (api.mesh/greased-line name params)
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
                gradient? (-> objects-data :skybox :gradient?)
                background? (-> objects-data :skybox :background)
                prev-and-gradient? (and (= :prev command)
                                        (-> (:data (slides (inc current-index))) :skybox :gradient?))
                _ (doseq [name (set/difference current-slide-object-names
                                               object-names-to-dispose
                                               (set prev-slide-object-names))]
                    (enable-component name (get-in slide [:data name])))
                anims (mapv #(api.animation/begin-direct-animation %) animations-data)
                _ (reset! slide-in-progress? true)
                _ (swap! current-running-anims #(vec (concat % anims)))
                channels (map :ch anims)
                pcs-animations (keep
                                 (fn [object-name]
                                   (let [object-slide-info (get-in slide [:data object-name])]
                                     (when (and (#{:pcs-text} (:type object-slide-info)))
                                       (api.animation/pcs-text-anim object-name object-slide-info))))
                                 object-names-from-slide-info)]
            (swap! current-running-anims #(vec (concat % pcs-animations)))
            ;;
            ;; (when (and (not prev-and-gradient?) (not background?))
            ;;  (a/<! (run-skybox-dissolve-animation objects-data)))
            ;;
            ;; (when (and (= :next command)
            ;;           background?
            ;;           (not (-> (:data (slides (dec current-index))) :skybox :background?)))
            ;;  (<! (api.animation/create-skybox->background-dissolve-anim :speed-factor 0.5)))
            ;;
            ;; (when (and (= :prev command)
            ;;           (not background?)
            ;;           (-> (:data (slides (inc current-index))) :skybox :background?))
            ;;  (<! (api.animation/create-background->skybox-dissolve-anim :objects-data objects-data)))
            ;;
            (when (or (and background?
                           (= :prev command)
                           (-> (:data (slides (inc current-index))) :skybox :background :color))
                      (and background?
                           (= :next command)
                           (> current-index 0)
                           (-> (:data (slides (dec current-index))) :skybox :background :color)))
              (do
                (println "background color changing")
                (some->> (api.animation/create-background->background-color-anim (:skybox objects-data))
                         (swap! current-running-anims conj))))
            ;;
            ;; (when (and background?
            ;;           (= :next command)
            ;;           (-> (:data (slides (dec current-index))) :skybox :background?))
            ;;  (api.animation/create-background->background-color-anim :objects-data objects-data))
            ;;
            ;; (when (and (= :next command)
            ;;           gradient?
            ;;           (-> (:data (slides (dec current-index))) :skybox :background?))
            ;;  (some-> (api.animation/create-background->gradient-anim :objects-data objects-data) a/<!))
            ;;
            ;; (when (and (= :prev command)
            ;;           background?
            ;;           (-> (:data (slides (inc current-index))) :skybox :gradient?))
            ;;  (some-> (api.animation/create-gradient->background-anim :objects-data objects-data) a/<!))


            ;; (doseq [c channels]
            ;;  (a/<! c))
            ;; (doseq [{:keys [ch]} pcs-animations]
            ;;  (a/<! ch))

            (doseq [{:keys [ch]} @current-running-anims]
              (a/<! ch))

            (reset! slide-in-progress? false)
            (recur current-index))
          (recur index))))))
