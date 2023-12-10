(ns immersa.scene.core
  (:require
    [applied-science.js-interop :as j]
    [cljs.core.async :as a :refer [go go-loop <!]]
    [clojure.set :as set]
    [immersa.common.utils :as common.utils]
    [immersa.events :as events]
    [immersa.scene.api :as api :refer [v3 v4]]
    [immersa.scene.macros :as m]
    [re-frame.core :refer [dispatch]]))

(defn reset-camera []
  (let [cam (api/active-camera)]
    (j/assoc! cam
              :position (api/clone (j/get cam :init-position))
              :rotation (api/clone (j/get cam :init-rotation)))))

(defn- create-box [name params]
  (let [columns 6
        rows 1
        face-uv (js/Array. columns)
        _ (dotimes [i columns]
            (aset face-uv i (v4 (/ i columns) 0 (/ (+ i 1) columns) (/ 1 rows))))
        texture (api/texture "img/texture/numbers.jpg")
        mat (api/standard-mat "mat" :diffuse-texture texture)]
    (api/box name
             (assoc params
                    :face-uv face-uv
                    :wrap? true
                    :mat mat))))

(defn billboard [name & {:keys [text
                                position
                                rotation
                                width
                                height
                                visibility
                                color
                                scale
                                resolution-scale
                                font-weight
                                rect-height
                                rect-corner-radius
                                rect-background]
                         :or {width 1.2
                              height 1
                              scale 1
                              resolution-scale 5
                              color "white"
                              rect-height "2500px"
                              rect-corner-radius 500
                              rect-background "rgba(128, 128, 128, 0.4)"}}]
  (let [plane (api/plane name
                         :width width
                         :height height
                         :position position
                         :rotation rotation
                         :billboard-mode api/mesh-billboard-mode-all
                         :visibility visibility
                         :scale scale
                         :type :billboard)
        gui (api/gui-create-for-mesh plane :width (* resolution-scale 1024) :height (* resolution-scale 1024))
        text (api/gui-text-block (str name "-text-block")
                                 :text text
                                 :font-size-in-pixels (* 60 resolution-scale)
                                 :text-wrapping api/gui-text-wrapping-word-wrap
                                 :text-horizontal-alignment api/gui-horizontal-align-left
                                 :padding-left (* 50 resolution-scale)
                                 :color color
                                 :font-weight font-weight)
        rect (api/gui-rectangle (str name "-rect")
                                :corner-radius rect-corner-radius
                                :height rect-height
                                :background rect-background)]
    (api/add-prop-to-db name :children [plane gui text rect])
    (api/add-control gui rect)
    (api/add-control rect text)
    plane))

(defn create-focus-camera-anim [object-slide-info]
  (when-let [object-name (:focus object-slide-info)]
    (let [object (api/get-object-by-name object-name)
          focus-type (:type object-slide-info)
          _ (j/call object :computeWorldMatrix true)
          bounding-box (j/get (j/call object :getBoundingInfo) :boundingBox)
          diagonal-size (j/call-in bounding-box [:maximumWorld :subtract] (j/get bounding-box :minimumWorld))
          diagonal-size (j/call diagonal-size :length)
          camera (api/active-camera)
          {:keys [x y z]} (j/lookup (api/get-pos object))
          radius 1.5
          final-radius (* radius diagonal-size)
          final-position (v3 x y (- z final-radius))
          easing-function (api/cubic-ease api/easing-ease-in-out)
          position-animation (api/animation "camera-position-anim"
                                            :fps 60
                                            :target-prop "position"
                                            :from (j/get camera :position)
                                            :to final-position
                                            :data-type api/animation-type-v3
                                            :loop-mode api/animation-loop-cons
                                            :easing easing-function)
          target-animation (api/animation "camera-target-anim"
                                          :fps 60
                                          :target-prop "target"
                                          :from (j/call camera :getTarget)
                                          :to (case focus-type
                                                :left (v3 (+ x (* diagonal-size radius 0.35)) y z)
                                                :right (v3 (- x (* diagonal-size radius 0.35)) y z)
                                                (v3 x y z))
                                          :data-type api/animation-type-v3
                                          :loop-mode api/animation-loop-cons
                                          :easing easing-function)]
      [:camera [position-animation target-animation]])))

(defn create-position-animation [start end duration]
  (api/animation "position-animation"
                 :target-prop "position"
                 :duration duration
                 :from start
                 :to end
                 :data-type api/animation-type-v3
                 :loop-mode api/animation-loop-cons
                 :easing (api/cubic-ease api/easing-ease-in-out)))

(defn create-rotation-animation [start end duration]
  (api/animation "rotation-animation"
                 :target-prop "rotation"
                 :duration duration
                 :from start
                 :to end
                 :data-type api/animation-type-v3
                 :loop-mode api/animation-loop-cons
                 :easing (api/cubic-ease api/easing-ease-in-out)))

(defn- create-visibility-animation [start end duration]
  (api/animation "visibility-animation"
                 :target-prop "visibility"
                 :duration duration
                 :from start
                 :to end
                 :data-type :ANIMATIONTYPE_FLOAT
                 :loop-mode api/animation-loop-cons
                 :easing (api/cubic-ease api/easing-ease-in-out)))

(defn- create-alpha-animation [start end duration]
  (api/animation "alpha-animation"
                 :target-prop "alpha"
                 :duration duration
                 :from start
                 :to end
                 :data-type :ANIMATIONTYPE_FLOAT
                 :loop-mode api/animation-loop-cons
                 :easing (api/cubic-ease api/easing-ease-in-out)))

(defn- get-position-anim [object-slide-info object-name]
  (let [object (api/get-object-by-name object-name)
        start-pos (j/get object :position)
        end-pos (:position object-slide-info)]
    (cond
      (and start-pos end-pos (not (api/equals? start-pos end-pos)))
      [object-name (create-position-animation start-pos end-pos 1)]

      (and (= object-name :camera) (not (api/equals? start-pos (j/get object :init-position))))
      [object-name (create-position-animation start-pos (api/clone (j/get object :init-position)) 1)])))

(defn- get-rotation-anim [object-slide-info object-name]
  (let [object (api/get-object-by-name object-name)
        start-rotation (j/get object :rotation)
        end-rotation (:rotation object-slide-info)]
    (cond
      (and start-rotation end-rotation (not (api/equals? start-rotation end-rotation)))
      [object-name (create-rotation-animation start-rotation end-rotation 1)]

      (and (= object-name :camera) (not (api/equals? start-rotation (j/get object :init-rotation))))
      [object-name (create-rotation-animation start-rotation (api/clone (j/get object :init-rotation)) 1)])))

(defn- get-visibility-anim [object-slide-info object-name]
  (let [last-visibility (:visibility object-slide-info)
        init-visibility (j/get (api/get-object-by-name object-name) :visibility)]
    (when (and last-visibility (not= init-visibility last-visibility))
      [object-name (create-visibility-animation init-visibility last-visibility 1)])))

(defn- get-alpha-anim [object-slide-info object-name]
  (let [last-alpha (:alpha object-slide-info)
        init-alpha (j/get (api/get-object-by-name object-name) :alpha)]
    (when (and last-alpha (not= init-alpha last-alpha))
      [object-name (create-alpha-animation init-alpha last-alpha 1)])))

(defn- get-animations-from-slide-info [acc object-slide-info object-name]
  (reduce
    (fn [acc anim-type]
      (let [[object-name anim :as anim-vec] (case anim-type
                                              :position (get-position-anim object-slide-info object-name)
                                              :rotation (get-rotation-anim object-slide-info object-name)
                                              :visibility (get-visibility-anim object-slide-info object-name)
                                              :alpha (get-alpha-anim object-slide-info object-name)
                                              :focus (create-focus-camera-anim object-slide-info))]
        (cond-> acc
          (and (not= anim-type :focus) anim-vec)
          (conj anim-vec)

          (and (= anim-type :focus) anim-vec)
          (conj [object-name (first anim)] [object-name (second anim)]))))
    acc
    [:position :rotation :visibility :alpha :focus]))

(defn- notify-ui [index slides-count]
  (dispatch [::events/update-slide-info index slides-count]))

(defn get-slides []
  (let [slides [{:data {"immersa-text" {:type :text
                                        :text "IMMERSA"
                                        :font-size 72
                                        :font-family "Bellefair,serif"
                                        :line-spacing "10px"
                                        :alpha 0
                                        :color "white"
                                        :text-horizontal-alignment api/gui-horizontal-align-center
                                        :text-vertical-alignment api/gui-vertical-align-center}}}
                {:data {"immersa-text" {:type :text
                                        :alpha 1}
                        "immersa-text-2" {:type :text
                                          :text "A 3D Presentation Tool for the Web"
                                          :font-size 72
                                          :font-family "Bellefair,serif"
                                          :line-spacing "10px"
                                          :alpha 0
                                          :color "white"
                                          :text-horizontal-alignment api/gui-horizontal-align-center
                                          :text-vertical-alignment api/gui-vertical-align-center
                                          :padding-top "70%"}}}
                {:data {:camera {:focus "earth2"
                                 :type :center}
                        "immersa-text" {:type :text
                                        :alpha 0}
                        "immersa-text-2" {:type :text
                                          :alpha 1}}}
                {:data {"box" {:type :box
                               :position (v3 2 0 0)
                               :rotation (v3 0 2.4 0)
                               :visibility 0.5}
                        "immersa-text-2" {:type :text
                                          :alpha 0}}}

                {:data {"box" {:type :box
                               :position (v3 0 2 0)
                               :rotation (v3 1.2 2.3 4.1)
                               :visibility 1}}}

                {:data {:camera {:focus "box"
                                 :type :left}
                        "billboard-1" {:type :billboard
                                       :position (v3 3 2.3 0)
                                       :text "\n❖ 3D Immersive Experience\n\n❖ Web-Based Accessibility\n\n❖ AI-Powered Features\n\n"
                                       :scale 2
                                       :font-weight "bold"
                                       :visibility 1}
                        "box" {}}}

                {:data {:camera {:focus "box"
                                 :type :right}
                        "billboard-2" {:type :billboard
                                       :position (v3 -3 2.3 0)
                                       :text "❖ Ready-Made Templates\n\n❖ High-Performance Render\n\n❖ User-Friendly UI/UX"
                                       :scale 2
                                       :font-weight "bold"}
                        "box" {}}}

                {:data {:camera {:position (v3 0 0 -10)
                                 :rotation (v3 0 0 0)}
                        "box" {}}}

                {:data {"immersa-text-3" {:type :text
                                          :text "✦Enjoy the Immersive Experience✦"
                                          :font-size 72
                                          :font-family "Bellefair,serif"
                                          :line-spacing "10px"
                                          :alpha 1
                                          :color "white"
                                          :text-horizontal-alignment api/gui-horizontal-align-center
                                          :text-vertical-alignment api/gui-vertical-align-center}}}

                {:data {:camera {:position (v3 0 0 50)}
                        "immersa-text-3" {:alpha 0}}}]
        slides-vec (vec (map-indexed #(assoc %2 :index %1) slides))
        props-to-copy [:type :position :rotation :visibility]
        clone-if-exists (fn [data]
                          (cond-> data
                            (:position data) (assoc :position (api/clone (:position data)))
                            (:rotation data) (assoc :rotation (api/clone (:rotation data)))))]
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

(comment
  (let [command-ch (a/chan (a/dropping-buffer 1))]
    (a/put! command-ch :next)
    (api/dispose-all (concat (api/get-objects-by-type "box")
                             (api/get-objects-by-type "billboard")
                             (api/get-objects-by-type "text")))
    (reset-camera)
    (api/detach-control (api/active-camera))
    (common.utils/remove-element-listeners)

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
                   next-index (case command
                                :next (inc index)
                                :prev (dec index))
                   slides (get-slides)]
               (if (and (>= next-index 0) (< next-index (count slides)))
                 (let [_ (notify-ui next-index (count slides))
                       slide (slides next-index)
                       objects-data (:data slide)
                       object-names-from-slide-info (set (conj (keys (:data slide)) :camera))
                       _ (when (object-names-from-slide-info :camera)
                           (api/update-active-camera))
                       object-names-from-objects (-> slide :objects keys)
                       objects-to-create (filter #(not (api/get-object-by-name %)) object-names-from-slide-info)
                       object-names-to-dispose (when (> next-index 0)
                                                 (let [prev-slide (slides (if (= command :next)
                                                                            (dec next-index)
                                                                            (inc next-index)))
                                                       prev-slide-object-names (-> prev-slide :data keys set)
                                                       current-slide-object-names (-> slide :data keys set)]
                                                   (set/difference prev-slide-object-names current-slide-object-names #{:camera})))

                       #_#__ (doseq [name object-names-to-dispose]
                               (api/dispose name))
                       _ (doseq [name objects-to-create]
                           (let [params (get objects-data name)
                                 type (:type params)
                                 params (dissoc params :type)]
                             (case type
                               :box (create-box name params)
                               :text3D (api/text name params)
                               :text (api/add-control
                                       (api/get-advanced-texture)
                                       (api/gui-text-block name params))
                               :billboard (billboard name params)
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
                                                   max-fps (apply max (map (j/get :framePerSecond) animations))]
                                               (assoc acc name {:target (api/get-object-by-name name)
                                                                :animations animations
                                                                :from 0
                                                                :to max-fps})))
                                           {}
                                           (group-by first animations)))
                       channels (mapv #(api/begin-direct-animation %) animations-data)]
                   (doseq [c channels]
                     (a/<! c))
                   (recur next-index))
                 (recur index))))))

(defn register-before-render []
  (let [delta (api/get-delta-time)]
    (some-> (api/get-object-by-name "sky-box") (j/update-in! [:rotation :y] #(+ % (* 0.008 delta))))
    (some-> (api/get-object-by-name "earth2") (j/update-in! [:rotation :y] #(- % (* 0.05 delta))))
    (some-> (api/get-object-by-name "cloud") (j/update-in! [:rotation :y] #(- % (* 0.07 delta))))))

(defn when-scene-ready [scene]
  (api/scene-clear-color api/color-white)
  (j/assoc-in! (api/get-object-by-name "sky-box") [:rotation :y] js/Math.PI)
  (api/advanced-dynamic-texture)
  (j/call scene :registerBeforeRender (fn [] (register-before-render))))

(defn start-scene [canvas]
  (let [engine (api/create-engine canvas)
        scene (api/create-scene engine)
        camera (api/create-free-camera "free-camera" :position (v3 0 0 -10))
        light (api/hemispheric-light "light")
        light2 (api/directional-light "light2"
                                      :position (v3 20)
                                      :dir (v3 -1 -2 0))
        ground-material (api/grid-mat "grid-mat"
                                      :major-unit-frequency 5
                                      :minor-unit-visibility 0.45
                                      :grid-ratio 2
                                      :back-face-culling? false
                                      :main-color (api/color 1 1 1)
                                      :line-color (api/color 1 1 1)
                                      :opacity 0.98)
        ground (api/create-ground "ground"
                                  :width 50
                                  :height 50
                                  :mat ground-material)
        sky-box (api/create-sky-box)]
    (j/assoc! light :intensity 0.7)
    (j/call camera :setTarget (v3))
    (j/call camera :attachControl canvas false)
    (j/call engine :runRenderLoop #(j/call scene :render))
    (j/call scene :executeWhenReady #(when-scene-ready scene))))

(comment
  (api/dispose "light2")
  (api/show-debug)
  (reset-camera)
  (let [gl (api/glow-layer "gl")
        mat (api/standard-mat "mat2"
                              :diffuse-texture (api/texture "img/texture/earth/diffuse2.png")
                              :emissive-texture (api/texture "img/texture/earth/emmisive.jpeg")
                              :specular-texture (api/texture "img/texture/earth/specular.jpeg")
                              :bump-texture (api/texture "img/texture/earth/bump.jpeg"))
        mat-clouds (api/standard-mat "clouds"
                                     :opacity-texture (api/texture "img/texture/earth/clouds2.jpg")
                                     :get-alpha-from-rgb? true)
        tn (api/transform-node "earth-node" :position (v3 0 -0.7 -8.5))
        sp (api/sphere "earth2"
                       :mat mat
                       :scale 1.2
                       :rotation (v3 0 0 js/Math.PI))
        clouds (api/sphere "cloud"
                           :mat mat-clouds
                           :scale 1.21
                           :rotation (v3 0 0 js/Math.PI))
        hl (api/highlight-layer "hl2"
                                :blur-vertical-size 3
                                :blur-horizontal-size 3)]
    (api/add-children tn sp clouds)
    ;(j/call hl :addExcludedMesh (api/get-object-by-name "sky-box"))
    ;(j/call gl :addIncludedOnlyMesh sp)
    (j/call hl :addMesh clouds (api/color 0.3 0.74 0.94 0.82))
    ;(j/assoc! clouds :parent tn)
    ;(j/assoc! sp :parent tn)
    )

  (do
    (api/dispose-engine)
    (start-scene (js/document.getElementById "renderCanvas")))

  (do
    (api/dispose "immersa-text" "immersa-text-2")
    (api/add-control
      (api/get-advanced-texture)
      (api/gui-text-block "immersa-text"
                          :text "IMMERSA"
                          :font-size 72
                          :alpha 0.5
                          ;:font-size-in-pixels (* 60 2)
                          :color "white"
                          :text-horizontal-alignment api/gui-horizontal-align-center
                          :text-vertical-alignment api/gui-vertical-align-center))
    (api/add-control
      (api/get-advanced-texture)
      (api/gui-text-block "immersa-text-2"
                          :text "A 3D Presentation Tool"
                          :font-size 72
                          :padding-top 200
                          ;:font-size-in-pixels (* 60 2)
                          :color "white"
                          :text-horizontal-alignment api/gui-horizontal-align-center
                          :text-vertical-alignment api/gui-vertical-align-center)))
  )
