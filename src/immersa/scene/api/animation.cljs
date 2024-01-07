(ns immersa.scene.api.animation
  (:require
    ["@babylonjs/core/Animations/animatable"]
    ["@babylonjs/core/Animations/animation" :refer [Animation]]
    ["@babylonjs/core/Animations/easing" :refer [CubicEase EasingFunction]]
    [applied-science.js-interop :as j]
    [cljs.core.async :as a]
    [immersa.scene.api.camera :as api.camera]
    [immersa.scene.api.constant :as api.const]
    [immersa.scene.api.core :as api.core :refer [v3]]
    [immersa.scene.api.material :as api.material])
  (:require-macros
    [immersa.scene.macros :as m]))

(defn cubic-ease [mode]
  (doto (CubicEase.)
    (j/call :setEasingMode (j/get EasingFunction mode))))

(defn animation [name & {:keys [target-prop
                                delay
                                fps
                                data-type
                                loop-mode
                                easing
                                from
                                to
                                duration
                                keys]
                         :or {fps 30
                              duration 1.0}}]
  (let [keys (or keys (and from to [{:frame 0 :value from}
                                    {:frame (* duration fps) :value to}]))
        anim (Animation. name target-prop fps (j/get Animation data-type) (j/get Animation loop-mode))]
    (m/cond-doto anim
      duration (j/assoc! :duration duration)
      delay (j/assoc! :delay delay)
      easing (j/call :setEasingFunction easing)
      keys (j/call :setKeys (clj->js keys)))))

(defn begin-direct-animation [& {:keys [target
                                        animations
                                        from
                                        to
                                        loop?
                                        speed-ratio
                                        on-animation-end
                                        delay]
                                 :or {from 0
                                      loop? false
                                      speed-ratio 1.0}}]
  (let [p (a/promise-chan)
        on-animation-end (fn []
                           (when on-animation-end
                             (on-animation-end target))
                           (a/put! p true))
        f #(j/call-in api.core/db [:scene :beginDirectAnimation]
                      target
                      (clj->js (if (vector? animations)
                                 animations
                                 [animations]))
                      from
                      to
                      loop?
                      speed-ratio
                      on-animation-end)]
    (if (and delay (> delay 0))
      (js/setTimeout f delay)
      (f))
    p))

(defn create-position-animation [{:keys [start end duration delay]}]
  (let [duration (or duration 1.0)]
    (animation "position-animation"                         ;
               :target-prop "position"
               :duration duration
               :delay delay                                 ;
               :from start
               :to end
               :data-type api.const/animation-type-v3
               :loop-mode api.const/animation-loop-cons
               :easing (cubic-ease api.const/easing-ease-in-out))))

(defn create-multiple-position-animation [{:keys [start end duration delay fps]
                                           :or {duration 1.0
                                                fps 30}}]
  (let [from {:frame 0 :value start}
        n-positions (count end)
        keys (vec
               (cons
                 from
                 (map
                   (fn [frame value]
                     {:frame frame :value value})
                   (rest (take (inc n-positions) (iterate (partial + (/ (* fps duration) n-positions)) 0)))
                   end)))]
    (animation "multiple-position-animation"
               :target-prop "position"
               :fps fps
               :duration duration
               :delay delay
               :keys keys
               :data-type api.const/animation-type-v3
               :loop-mode api.const/animation-loop-cons
               :easing (cubic-ease api.const/easing-ease-in-out))))

(defn create-rotation-animation [{:keys [start end duration delay]
                                  :or {duration 1.0}}]
  (animation "rotation-animation"
             :target-prop "rotation"
             :duration duration
             :delay delay
             :from start
             :to end
             :data-type api.const/animation-type-v3
             :loop-mode api.const/animation-loop-cons
             :easing (cubic-ease api.const/easing-ease-in-out)))

(defn create-visibility-animation [{:keys [start end duration delay]
                                    :or {duration 1.0}}]
  (animation "visibility-animation"
             :target-prop "visibility"
             :duration duration
             :delay delay
             :from start
             :to end
             :data-type api.const/animation-type-float
             :loop-mode api.const/animation-loop-cons
             :easing (cubic-ease api.const/easing-ease-in-out)))

(defn create-alpha-animation [{:keys [start end duration delay]
                               :or {duration 1.0}}]
  (animation "alpha-animation"
             :target-prop "alpha"
             :duration duration
             :delay delay
             :from start
             :to end
             :data-type api.const/animation-type-float
             :loop-mode api.const/animation-loop-cons
             :easing (cubic-ease api.const/easing-ease-in-out)))

(defn create-camera-target-anim [{:keys [camera target duration delay]
                                  :or {duration 1.0}}]
  (animation "camera-target-anim"
             :delay delay
             :target-prop "target"
             :duration duration
             :from (j/call camera :getTarget)
             :to target
             :data-type api.const/animation-type-v3
             :loop-mode api.const/animation-loop-cons
             :easing (cubic-ease api.const/easing-ease-in-out)))

(defn create-focus-camera-anim [object-slide-info]
  (when-let [object-name (:focus object-slide-info)]
    (let [object (api.core/get-object-by-name object-name)
          focus-type (:type object-slide-info)
          _ (j/call object :computeWorldMatrix true)
          bounding-box (j/get (j/call object :getBoundingInfo) :boundingBox)
          diagonal-size (j/call-in bounding-box [:maximumWorld :subtract] (j/get bounding-box :minimumWorld))
          diagonal-size (j/call diagonal-size :length)
          camera (api.camera/active-camera)
          {:keys [x y z]} (j/lookup (api.core/get-pos object))
          radius 1.5
          final-radius (* radius diagonal-size)
          final-position (v3 x y (- z final-radius))
          easing-function (cubic-ease api.const/easing-ease-in-out)
          position-animation (animation "camera-position-anim"
                                        :delay (:delay object-slide-info)
                                        :target-prop "position"
                                        :from (j/get camera :position)
                                        :to final-position
                                        :data-type api.const/animation-type-v3
                                        :loop-mode api.const/animation-loop-cons
                                        :easing easing-function)
          target-animation (animation "camera-target-anim"
                                      :delay (:delay object-slide-info)
                                      :target-prop "target"
                                      :from (j/call camera :getTarget)
                                      :to (case focus-type
                                            :left (v3 (+ x (* diagonal-size radius 0.35)) y z)
                                            :right (v3 (- x (* diagonal-size radius 0.35)) y z)
                                            (v3 x y z))
                                      :data-type api.const/animation-type-v3
                                      :loop-mode api.const/animation-loop-cons
                                      :easing easing-function)]
      [:camera [position-animation target-animation]])))

(defn create-skybox-dissolve-anim [& {:keys [skybox-path
                                             speed-factor]
                                      :or {speed-factor 1}}]
  (let [p (a/promise-chan)
        dissolve (atom 0)
        dissolve-fn-name "dissolve-skybox"
        mat (api.core/get-object-by-name "skybox-shader")
        current-skybox-path (j/get mat :skybox-path)
        dissolve-fn (fn []
                      (let [dissolve (swap! dissolve + (* (api.core/get-delta-time) speed-factor))]
                        (if (<= dissolve 1.5)
                          (j/call mat :setFloat "dissolve" dissolve)
                          (do
                            (j/assoc! mat :skybox-path skybox-path)
                            (api.core/remove-before-render-fn dissolve-fn-name)
                            (a/put! p true)))))]
    (j/call mat :setTexture "skybox1" (api.core/cube-texture :root-url current-skybox-path))
    (j/call mat :setTexture "skybox2" (api.core/cube-texture :root-url skybox-path))
    (j/call mat :setFloat "dissolve" 0)
    (api.core/register-before-render-fn dissolve-fn-name dissolve-fn)
    p))

(defn create-sky-sphere-dissolve-anim []
  (let [p (a/promise-chan)
        dissolve (atom 0)
        dissolve-fn-name "dissolve-sky-sphere"
        sphere (api.core/get-object-by-name "sky-sphere")
        mat (api.core/get-object-by-name "sky-sphere-mat")
        speed-factor 2
        dissolve-fn (fn []
                      (let [dissolve (swap! dissolve + (* (api.core/get-delta-time) speed-factor))]
                        (if (<= dissolve 1.0)
                          (j/call mat :setFloat "u_visibility" dissolve)
                          (do
                            (j/assoc! (api.core/get-object-by-name "sky-box") :visibility 0)
                            (api.core/remove-before-render-fn dissolve-fn-name)
                            (a/put! p true)))))]
    (j/assoc! sphere :visibility 1)
    (api.core/register-before-render-fn dissolve-fn-name dissolve-fn)
    p))

(defn create-reverse-sky-sphere-dissolve-anim []
  (let [p (a/promise-chan)
        dissolve (atom 1)
        dissolve-fn-name "reverse-dissolve-sky-sphere"
        sphere (api.core/get-object-by-name "sky-sphere")
        mat (api.core/get-object-by-name "sky-sphere-mat")
        speed-factor 2
        dissolve-fn (fn []
                      (let [dissolve (swap! dissolve - (* (api.core/get-delta-time) speed-factor))]
                        (if (>= dissolve 0.0)
                          (j/call mat :setFloat "u_visibility" dissolve)
                          (do
                            (j/assoc! (api.core/get-object-by-name "sky-box") :visibility 1)
                            (j/assoc! sphere :visibility 0)
                            (api.core/remove-before-render-fn dissolve-fn-name)
                            (a/put! p true)))))]
    (api.core/register-before-render-fn dissolve-fn-name dissolve-fn)
    p))

(defn pcs-text-anim [name & {:keys [text
                                    duration
                                    position
                                    font-size
                                    point-size
                                    sample-factor
                                    simplify-threshold
                                    down-scale
                                    fps
                                    color
                                    rand-range
                                    font
                                    visibility
                                    delay]
                             :or {point-size 1
                                  font-size 120
                                  down-scale 100
                                  sample-factor 0.25
                                  simplify-threshold 0
                                  rand-range [-10 10]
                                  fps 30
                                  duration 2.0
                                  font :big-caslon}
                             :as opts}]
  (let [p (a/promise-chan)
        font (api.core/get-p5-font font)
        points (j/call font :textToPoints text 0 0 font-size #js {:sampleFactor sample-factor
                                                                  :simplifyThreshold simplify-threshold})
        pcs (api.core/pcs (str name "-pcs") :point-size point-size)]
    (doseq [p points]
      (api.core/add-points pcs 1 (fn [particle]
                                   (j/assoc! particle
                                             :position (v3 (/ (j/get p :x) down-scale)
                                                           (/ (- (j/get p :y)) down-scale)
                                                           0)
                                             :color (or color
                                                        (api.core/color (js/Math.random)
                                                                        (js/Math.random)
                                                                        (js/Math.random)))))))
    (api.core/build-mesh-async
      pcs
      (fn [mesh]
        (api.core/add-node-to-db name mesh (assoc opts :type :pcs-text))
        (api.core/add-prop-to-db name :pcs pcs)
        (m/cond-doto mesh
          position (j/assoc! :position position)
          visibility (j/assoc! :visibility visibility))
        (let [end-positions (j/call mesh :getPositionData)
              end-positions-len (j/get end-positions :length)
              points-count (/ end-positions-len 3)
              [rand1 rand2] rand-range
              init-positions (into-array
                               (map
                                 (fn [_]
                                   (api.core/rand-range rand1 rand2))
                                 (range end-positions-len)))
              new-positions (js/Array. end-positions-len)
              interpolate-factor #js {:value 0}
              anim (animation (str name "-pcs-morph-animation")
                              :target-prop "value"
                              :from 0
                              :to 1
                              :duration duration
                              :fps fps
                              :data-type api.const/animation-type-float
                              :loop-mode api.const/animation-loop-cons
                              :easing (cubic-ease api.const/easing-ease-in-out))]
          (api.core/register-before-render-fn
            (str name "-pcs-morph-before-render")
            (fn []
              (let [inter (j/get interpolate-factor :value)]
                (doseq [p (range 0 points-count)]
                  (doseq [axis (range 0 3)]
                    (let [index (+ (* 3 p) axis)
                          start-value (j/get init-positions index)
                          end-value (j/get end-positions index)]
                      (j/assoc! new-positions index (api.core/lerp start-value end-value inter))))))
              (api.core/update-vertices-data mesh new-positions)))
          (begin-direct-animation
            :delay delay
            :target interpolate-factor
            :animations anim
            :from 0
            :to (* fps duration)
            :on-animation-end (fn []
                                (a/put! p true)
                                (api.core/update-vertices-data mesh end-positions)
                                (api.core/remove-before-render-fn (str name "-pcs-morph-before-render")))))))
    p))

(comment
  (api.core/dispose (api.core/get-object-by-name "text-dots"))
  (pcs-text-anim "text-dots"
                 {:type :pcs-text
                  :text "      Welcome to the\n\n\n\n\n\n\n\nFuture of Presentation"
                  :visibility 1
                  :duration 0.0
                  :point-size 5
                  :rand-range [-10 20]
                  :position (v3 -5.5 1 9)
                  :color api.const/color-white})
  )
