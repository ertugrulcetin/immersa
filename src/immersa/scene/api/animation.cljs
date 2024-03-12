(ns immersa.scene.api.animation
  (:require
    ["@babylonjs/core/Animations/animatable"]
    ["@babylonjs/core/Animations/animation" :refer [Animation]]
    ["@babylonjs/core/Animations/easing" :refer [CubicEase EasingFunction]]
    [applied-science.js-interop :as j]
    [cljs.core.async :as a]
    [clojure.string :as str]
    [immersa.scene.api.camera :as api.camera]
    [immersa.scene.api.constant :as api.const]
    [immersa.scene.api.core :as api.core :refer [v3]])
  (:require-macros
    [immersa.scene.macros :as m]))

(def fps 30)

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
        animations (if (vector? animations)
                     animations
                     [animations])
        finished? (atom false)
        f #(let [anim (j/call-in api.core/db [:scene :beginDirectAnimation]
                                 target
                                 (clj->js animations)
                                 from
                                 to
                                 loop?
                                 speed-ratio
                                 on-animation-end)]
             (j/call-in anim [:onAnimationEndObservable :add] (fn []
                                                                (reset! finished? true)
                                                                (when on-animation-end
                                                                  (on-animation-end))))
             anim)
        time-out-fn-id (atom nil)]
    (if (and delay (> delay 0))
      (reset! time-out-fn-id (js/setTimeout f delay))
      (f))
    {:ch p
     :finished? finished?
     :force-finish-fn (fn []
                        (when-not @finished?
                          (when @time-out-fn-id
                            (js/clearTimeout @time-out-fn-id))
                          (j/call-in api.core/db [:scene :stopAnimation] target)
                          (doseq [anim animations]
                            (let [last-val (-> anim (j/call :getKeys) last (j/get :value))]
                              (if (str/includes? (j/get anim :targetProperty) ".")
                                (let [props (str/split (j/get anim :targetProperty) #"\.")]
                                  (->> props
                                       drop-last
                                       (reduce
                                         (fn [target prop]
                                           (aget target prop))
                                         target)
                                       (#(aset % (last props) last-val))))
                                (j/assoc! target (j/get anim :targetProperty) last-val))))))}))

(defn- get-anim-keys [{:keys [start end duration]}]
  (when (vector? end)
    (let [from {:frame 0 :value start}
          n-positions (count end)]
      (->> (map
             (fn [frame value]
               {:frame frame :value value})
             (rest (take (inc n-positions) (iterate (partial + (/ (* fps duration) n-positions)) 0)))
             end)
           (cons from)
           vec))))

(defn create-position-animation [{:keys [start end duration delay]}]
  (let [duration (or duration 1.0)
        keys (get-anim-keys {:start start
                             :end end
                             :duration duration})]
    (animation "position-animation"
               (cond-> {:target-prop "position"
                        :duration duration
                        :delay delay
                        :data-type api.const/animation-type-v3
                        :loop-mode api.const/animation-loop-cons
                        :easing (cubic-ease api.const/easing-ease-in-out)}
                 keys (assoc :keys keys)
                 (nil? keys) (assoc :from start :to end)))))

(defn create-rotation-animation [{:keys [start end duration delay]}]
  (let [duration (or duration 1.0)
        keys (get-anim-keys {:start start
                             :end end
                             :duration duration})]
    (animation "rotation-animation"
               (cond-> {:target-prop "rotation"
                        :duration duration
                        :delay delay
                        :data-type api.const/animation-type-v3
                        :loop-mode api.const/animation-loop-cons
                        :easing (cubic-ease api.const/easing-ease-in-out)}
                 keys (assoc :keys keys)
                 (nil? keys) (assoc :from start :to end)))))

(defn create-scale-animation [{:keys [start end duration delay]}]
  (let [duration (or duration 1.0)
        keys (get-anim-keys {:start start
                             :end end
                             :duration duration})]
    (animation "scale-animation"
               (cond-> {:target-prop "scaling"
                        :duration duration
                        :delay delay
                        :data-type api.const/animation-type-v3
                        :loop-mode api.const/animation-loop-cons
                        :easing (cubic-ease api.const/easing-ease-in-out)}
                 keys (assoc :keys keys)
                 (nil? keys) (assoc :from start :to end)))))

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

(defn create-image-visibility-animation [{:keys [start end duration delay]
                                          :or {duration 1.0}}]
  (animation "image-visibility-animation"
             :target-prop "material.alpha"
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

(defn create-camera-target-anim [{:keys [camera target duration delay]}]
  (let [duration (or duration 1.0)
        start (j/call camera :getTarget)
        end target
        keys (get-anim-keys {:start start
                             :end end
                             :duration duration})]
    (animation "camera-target-anim"
               (cond-> {:target-prop "target"
                        :duration duration
                        :delay delay
                        :data-type api.const/animation-type-v3
                        :loop-mode api.const/animation-loop-cons
                        :easing (cubic-ease api.const/easing-ease-in-out)}
                 keys (assoc :keys keys)
                 (nil? keys) (assoc :from start :to end)))))

(defn run-camera-focus-anim [mesh locked?]
  (when-not locked?
    (let [duration 0.5
          _ (j/call mesh :computeWorldMatrix true)
          bounding-box (j/get (j/call mesh :getBoundingInfo) :boundingBox)
          diagonal-size (j/call-in bounding-box [:maximumWorld :subtract] (j/get bounding-box :minimumWorld))
          diagonal-size (j/call diagonal-size :length)
          camera (api.camera/active-camera)
          camera-pos (api.core/clone (j/get camera :position))
          mesh-world-center (j/get bounding-box :centerWorld)
          radius-factor -1.5
          final-radius (* radius-factor diagonal-size)
          direction (j/get (j/call camera :getForwardRay) :direction)
          final-position (j/call mesh-world-center :add (j/call direction :scale final-radius))
          easing-function (cubic-ease api.const/easing-ease-in)
          #_#_position-animation (animation "camera-position-anim"
                                            :duration duration
                                            :target-prop "position"
                                            :from camera-pos
                                            :to final-position
                                            :data-type api.const/animation-type-v3
                                            :loop-mode api.const/animation-loop-cons
                                            :easing easing-function)
          target-animation (animation "camera-target-anim"
                                      :duration duration
                                      :target-prop "target"
                                      :from (j/call camera :getTarget)
                                      :to mesh-world-center
                                      :data-type api.const/animation-type-v3
                                      :loop-mode api.const/animation-loop-cons
                                      :easing easing-function)]
      (begin-direct-animation :target camera
                              :to (* fps duration)
                              :animations [#_position-animation target-animation]))))

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

(defn create-skybox->background-dissolve-anim [& {:keys [background duration]
                                                  :or {duration 1.0}}]
  (let [p (a/promise-chan)
        elapsed-time (atom 0)
        max-dissolve 1.1
        dissolve-fn-name "dissolve-skybox->background"
        mat (api.core/get-object-by-name "skybox-shader")
        skybox-path (j/get mat :skybox-path)
        _ (do
            (j/call mat :setTexture "skybox1" (api.core/cube-texture :root-url skybox-path))
            (j/call mat :setTexture "skybox2" (api.core/cube-texture :root-url skybox-path)))
        _ (j/assoc! mat :alpha 0)
        skybox-material (j/get-in api.core/db [:environment-helper :skybox :material])
        ground-material (j/get-in api.core/db [:environment-helper :ground :material])
        new-color (:color background)
        finish-fn (fn []
                    (api.core/remove-before-render-fn dissolve-fn-name)
                    (j/call mat :setFloat "dissolve" max-dissolve)
                    (j/call mat :setFloat "transparency" max-dissolve)
                    (a/put! p true))
        dissolve-fn (fn []
                      (let [elapsed-time (swap! elapsed-time + (api.core/get-delta-time))
                            dissolve-step (/ max-dissolve duration)
                            dissolve (* elapsed-time dissolve-step)]
                        (if (<= elapsed-time duration)
                          (do
                            (j/call mat :setFloat "dissolve" dissolve)
                            (j/call mat :setFloat "transparency" dissolve))
                          (finish-fn))))]
    (j/call mat :setFloat "dissolve" 0)
    (j/assoc! skybox-material :primaryColor new-color)
    (j/assoc! ground-material :primaryColor new-color)
    (api.core/register-before-render-fn dissolve-fn-name dissolve-fn)
    {:ch p
     :force-finish-fn finish-fn}))

(comment
  (j/get (api.core/get-object-by-name "skybox-shader") :skybox-path)
  )

(defn create-background->skybox-dissolve-anim [& {:keys [background duration]
                                                  :or {duration 1.0}}]
  (let [p (a/promise-chan)
        elapsed-time (atom 0)
        min-dissolve 0.0
        dissolve-fn-name "background->skybox-dissolve"
        mat (api.core/get-object-by-name "skybox-shader")
        skybox-1 (:image background)
        skybox-2 (j/get mat :skybox-path)
        finish-fn (fn []
                    (j/call mat :setFloat "dissolve" min-dissolve)
                    (j/call mat :setFloat "transparency" min-dissolve)
                    (j/assoc! mat :alpha 1)
                    (j/assoc! mat :skybox-path skybox-1)
                    (api.core/remove-before-render-fn dissolve-fn-name)
                    (a/put! p true))
        dissolve-fn (fn []
                      (let [elapsed-time (swap! elapsed-time + (api.core/get-delta-time))
                            dissolve (/ (- duration elapsed-time) duration)]
                        (if (>= dissolve min-dissolve)
                          (do
                            (j/call mat :setFloat "dissolve" dissolve)
                            (j/call mat :setFloat "transparency" dissolve))
                          (finish-fn))))]
    (j/call mat :setTexture "skybox1" (api.core/cube-texture :root-url skybox-1))
    (j/call mat :setTexture "skybox2" (api.core/cube-texture :root-url skybox-2))
    (api.core/register-before-render-fn dissolve-fn-name dissolve-fn)
    {:ch p
     :force-finish-fn finish-fn}))

(defn create-background->background-color-anim [{:keys [background duration]
                                                 :or {duration 1.0}}]
  (let [skybox-material (j/get-in api.core/db [:environment-helper :skybox :material])
        ground-material (j/get-in api.core/db [:environment-helper :ground :material])
        current-color (j/get skybox-material :primaryColor)
        new-color (:color background)]
    (api.core/lerp-colors {:start-color current-color
                           :end-color new-color
                           :duration duration
                           :targets [[skybox-material :primaryColor]
                                     [ground-material :primaryColor]]})))

(defn create-skybox-bg-image->bg-image-dissolve-anim [& {:keys [skybox-path
                                                                duration]}]
  (let [p (a/promise-chan)
        duration (or duration 1.0)
        elapsed-time (atom 0)
        dissolve-fn-name "dissolve-skybox"
        mat (api.core/get-object-by-name "skybox-shader")
        current-skybox-path (j/get mat :skybox-path)
        max-dissolve 1.1
        finish-fn (fn []
                    (api.core/remove-before-render-fn dissolve-fn-name)
                    (j/call mat :setFloat "dissolve" max-dissolve)
                    (j/assoc! mat :skybox-path skybox-path)
                    (a/put! p true))
        dissolve-fn (fn []
                      (let [elapsed-time (swap! elapsed-time + (api.core/get-delta-time))
                            dissolve-step (/ max-dissolve duration)
                            dissolve (* elapsed-time dissolve-step)]
                        (if (<= elapsed-time duration)
                          (j/call mat :setFloat "dissolve" dissolve)
                          (finish-fn))))]
    (j/call mat :setTexture "skybox1" (api.core/cube-texture :root-url current-skybox-path))
    (j/call mat :setTexture "skybox2" (api.core/cube-texture :root-url skybox-path))
    (j/call mat :setFloat "dissolve" 0)
    (api.core/register-before-render-fn dissolve-fn-name dissolve-fn)
    {:ch p
     :force-finish-fn finish-fn}))

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

(defn create-background->gradient-anim [& {:keys [speed-factor]
                                           :or {speed-factor 1}}]
  (let [p (a/promise-chan)
        skybox-material (j/get-in api.core/db [:environment-helper :skybox :material])
        ground-material (j/get-in api.core/db [:environment-helper :ground :material])
        current-color (j/get skybox-material :primaryColor)
        new-color (api.core/color 0)]
    (api.core/lerp-colors
      {:start-color current-color
       :end-color new-color
       :on-lerp (fn [r g b]
                  (j/assoc! skybox-material :primaryColor (api.core/color r g b))
                  (j/assoc! ground-material :primaryColor (api.core/color r g b)))
       :on-end (fn []
                 (a/put! p true)
                 (let [dissolve (atom 0)
                       dissolve-fn-name "dissolve-sky-sphere"
                       sphere (api.core/get-object-by-name "sky-sphere")
                       mat (api.core/get-object-by-name "sky-sphere-mat")
                       speed-factor 2
                       dissolve-fn (fn []
                                     (let [dissolve (swap! dissolve + (* (api.core/get-delta-time) speed-factor))]
                                       (if (<= dissolve 1.0)
                                         (j/call mat :setFloat "u_visibility" dissolve)
                                         (do
                                           (api.core/remove-before-render-fn dissolve-fn-name)
                                           (a/put! p true)))))]
                   (j/assoc! sphere :visibility 1)
                   (api.core/register-before-render-fn dissolve-fn-name dissolve-fn)))})
    p))

(defn create-gradient->background-anim [& {:keys [objects-data
                                                  speed-factor]
                                           :or {speed-factor 1}}]
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
                            (j/assoc! sphere :visibility 0)
                            (api.core/remove-before-render-fn dissolve-fn-name)
                            (let [skybox-material (j/get-in api.core/db [:environment-helper :skybox :material])
                                  ground-material (j/get-in api.core/db [:environment-helper :ground :material])
                                  current-color (j/get skybox-material :primaryColor)
                                  new-color (-> objects-data :skybox :background?)]
                              (api.core/lerp-colors
                                {:start-color current-color
                                 :end-color new-color
                                 :on-lerp (fn [r g b]
                                            (j/assoc! skybox-material :primaryColor (api.core/color r g b))
                                            (j/assoc! ground-material :primaryColor (api.core/color r g b)))
                                 :on-end #(a/put! p true)}))))))]
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
        pcs (api.core/pcs (str name "-pcs") :point-size point-size)
        force-finish-fn (atom nil)]
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
        (api.core/update-node-attr name :pcs pcs)
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
                              :easing (cubic-ease api.const/easing-ease-in-out))
              before-render-fn-name (str name "-pcs-morph-before-render")
              _ (api.core/register-before-render-fn
                  before-render-fn-name
                  (fn []
                    (let [inter (j/get interpolate-factor :value)]
                      (doseq [p (range 0 points-count)]
                        (doseq [axis (range 0 3)]
                          (let [index (+ (* 3 p) axis)
                                start-value (j/get init-positions index)
                                end-value (j/get end-positions index)]
                            (j/assoc! new-positions index (api.core/lerp start-value end-value inter))))))
                    (api.core/update-vertices-data mesh new-positions)))
              on-animation-end (fn []
                                 (api.core/update-vertices-data mesh end-positions)
                                 (api.core/remove-before-render-fn before-render-fn-name)
                                 (a/put! p true))]
          (begin-direct-animation
            :delay delay
            :target interpolate-factor
            :animations anim
            :from 0
            :to (* fps duration)
            :on-animation-end on-animation-end)
          (reset! force-finish-fn (fn []
                                    (j/call-in api.core/db [:scene :stopAnimation] interpolate-factor)
                                    (on-animation-end))))))
    {:ch p
     :force-finish-fn-atom force-finish-fn}))

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
                  :color (api.const/color-white)})
  )
