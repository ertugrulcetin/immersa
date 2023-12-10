(ns immersa.scene.api.animation
  (:require
    ["@babylonjs/core/Animations/animatable"]
    ["@babylonjs/core/Animations/animation" :refer [Animation]]
    ["@babylonjs/core/Animations/easing" :refer [CubicEase EasingFunction]]
    [applied-science.js-interop :as j]
    [cljs.core.async :as a]
    [immersa.scene.api.camera :as api.camera]
    [immersa.scene.api.constant :as api.const]
    [immersa.scene.api.core :as api.core :refer [v3]])
  (:require-macros
    [immersa.scene.macros :as m]))

(defn cubic-ease [mode]
  (doto (CubicEase.)
    (j/call :setEasingMode (j/get EasingFunction mode))))

(defn animation [name & {:keys [target-prop
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
                                 :or {loop? false
                                      speed-ratio 1.0}
                                 :as opts}]
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

(defn create-position-animation [{:keys [start end duration]
                                  :or {duration 1.0}}]
  (animation "position-animation"
             :target-prop "position"
             :duration duration
             :from start
             :to end
             :data-type api.const/animation-type-v3
             :loop-mode api.const/animation-loop-cons
             :easing (cubic-ease api.const/easing-ease-in-out)))

(defn create-rotation-animation [{:keys [start end duration]
                                  :or {duration 1.0}}]
  (animation "rotation-animation"
             :target-prop "rotation"
             :duration duration
             :from start
             :to end
             :data-type api.const/animation-type-v3
             :loop-mode api.const/animation-loop-cons
             :easing (cubic-ease api.const/easing-ease-in-out)))

(defn create-visibility-animation [{:keys [start end duration]
                                    :or {duration 1.0}}]
  (animation "visibility-animation"
             :target-prop "visibility"
             :duration duration
             :from start
             :to end
             :data-type api.const/animation-type-float
             :loop-mode api.const/animation-loop-cons
             :easing (cubic-ease api.const/easing-ease-in-out)))

(defn create-alpha-animation [{:keys [start end duration]
                               :or {duration 1.0}}]
  (animation "alpha-animation"
             :target-prop "alpha"
             :duration duration
             :from start
             :to end
             :data-type api.const/animation-type-float
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
                                        :fps 60
                                        :target-prop "position"
                                        :from (j/get camera :position)
                                        :to final-position
                                        :data-type api.const/animation-type-v3
                                        :loop-mode api.const/animation-loop-cons
                                        :easing easing-function)
          target-animation (animation "camera-target-anim"
                                      :fps 60
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
