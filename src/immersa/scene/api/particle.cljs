(ns immersa.scene.api.particle
  (:require
    ["@babylonjs/core/Particles/gpuParticleSystem" :refer [GPUParticleSystem]]
    ["@babylonjs/core/Particles/particleSystem" :refer [ParticleSystem]]
    [applied-science.js-interop :as j]
    [immersa.scene.api.core :as api.core :refer [v3]])
  (:require-macros
    [immersa.scene.macros :as m]))

(defn- add-gradients [particle-system {:keys [size-gradients
                                              color-gradients
                                              velocity-gradients
                                              limit-velocity-gradients
                                              angular-speed-gradients
                                              emit-rate-gradients
                                              life-time-gradients
                                              start-size-gradients]}]
  (doseq [args size-gradients]
    (apply j/call particle-system :addSizeGradient args))
  (doseq [args color-gradients]
    (apply j/call particle-system :addColorGradient args))
  (doseq [args velocity-gradients]
    (apply j/call particle-system :addVelocityGradient args))
  (doseq [args limit-velocity-gradients]
    (apply j/call particle-system :addLimitVelocityGradient args))
  (doseq [args angular-speed-gradients]
    (apply j/call particle-system :addAngularSpeedGradient args))
  (doseq [args emit-rate-gradients]
    (apply j/call particle-system :addEmitRateGradient args))
  (doseq [args life-time-gradients]
    (apply j/call particle-system :addLifeTimeGradient args))
  (doseq [args start-size-gradients]
    (apply j/call particle-system :addStartSizeGradient args)))

(defn create-particle-system [name & {:keys [capacity
                                             gpu?
                                             particle-texture
                                             emitter
                                             min-emit-box
                                             max-emit-box
                                             color1
                                             color2
                                             color-dead
                                             min-size
                                             max-size
                                             min-life-time
                                             max-life-time
                                             emit-rate
                                             blend-mode
                                             gravity
                                             direction1
                                             direction2
                                             pre-warm-step-offset
                                             pre-warm-cycles
                                             min-angular-speed
                                             max-angular-speed
                                             min-emit-power
                                             max-emit-power
                                             min-initial-rotation
                                             max-initial-rotation
                                             local?
                                             target-stop-duration
                                             dispose-on-stop?
                                             update-speed
                                             noise-texture
                                             noise-strength
                                             texture-mask
                                             limit-velocity-damping]
                                      :or {capacity 100}
                                      :as opts}]
  (let [particle-system (if gpu?
                          (GPUParticleSystem. name capacity)
                          (ParticleSystem. name capacity))]
    (api.core/add-node-to-db name particle-system (assoc opts :type :particle))
    (add-gradients particle-system opts)
    (m/cond-doto particle-system
      particle-texture (j/assoc! :particleTexture particle-texture)
      emitter (j/assoc! :emitter emitter)
      min-emit-box (j/assoc! :minEmitBox min-emit-box)
      max-emit-box (j/assoc! :maxEmitBox max-emit-box)
      color1 (j/assoc! :color1 color1)
      color2 (j/assoc! :color2 color2)
      color-dead (j/assoc! :colorDead color-dead)
      min-size (j/assoc! :minSize min-size)
      max-size (j/assoc! :maxSize max-size)
      min-life-time (j/assoc! :minLifeTime min-life-time)
      max-life-time (j/assoc! :maxLifeTime max-life-time)
      emit-rate (j/assoc! :emitRate emit-rate)
      blend-mode (j/assoc! :blendMode blend-mode)
      gravity (j/assoc! :gravity gravity)
      direction1 (j/assoc! :direction1 direction1)
      direction2 (j/assoc! :direction2 direction2)
      min-angular-speed (j/assoc! :minAngularSpeed min-angular-speed)
      max-angular-speed (j/assoc! :maxAngularSpeed max-angular-speed)
      min-emit-power (j/assoc! :minEmitPower min-emit-power)
      max-emit-power (j/assoc! :maxEmitPower max-emit-power)
      update-speed (j/assoc! :updateSpeed update-speed)
      noise-texture (j/assoc! :noiseTexture noise-texture)
      noise-strength (j/assoc! :noiseStrength noise-strength)
      pre-warm-step-offset (j/assoc! :preWarmStepOffset pre-warm-step-offset)
      pre-warm-cycles (j/assoc! :preWarmCycles pre-warm-cycles)
      min-initial-rotation (j/assoc! :minInitialRotation min-initial-rotation)
      max-initial-rotation (j/assoc! :maxInitialRotation max-initial-rotation)
      (some? local?) (j/assoc! :isLocal local?)
      target-stop-duration (j/assoc! :targetStopDuration (if update-speed
                                                           (/ target-stop-duration (/ 0.01 update-speed))
                                                           target-stop-duration))
      dispose-on-stop? (j/assoc! :disposeOnStop dispose-on-stop?)
      texture-mask (j/assoc! :textureMask texture-mask)
      limit-velocity-damping (j/assoc! :limitVelocityDamping limit-velocity-damping))))

(defn start
  ([ps]
   (j/call ps :start))
  ([ps delay]
   (j/call ps :start delay)))

(defn stop [ps]
  (j/call ps :stop))

(defn reset [ps]
  (j/call ps :reset))

(defn circle-move [name & {:keys [position
                                  target-stop-duration]
                           :or {position (v3 0 0 0)
                                target-stop-duration 2}}]
  (let [emitter-position (v3 0 0 0)
        before-render-fn (str name "-circle-move-before-render")
        ps (create-particle-system name
                                   :particle-texture (api.core/texture "img/texture/star_04.png")
                                   :capacity 2000
                                   :min-life-time 0.3
                                   :max-life-time 0.5
                                   :color1 (api.core/color 0.7 0.8 1.0 1.0)
                                   :color2 (api.core/color 0.2 0.5 1.0 1.0)
                                   :color-dead (api.core/color 0 0 0.2 0.0)
                                   :min-size 0.1
                                   :max-size 0.3
                                   :gravity (v3 0 -9.81 0)
                                   :min-angular-speed 0
                                   :max-angular-speed Math/PI
                                   :min-emit-power 1
                                   :max-emit-power 3
                                   :update-speed 0.005
                                   :emit-rate 1000
                                   :target-stop-duration target-stop-duration
                                   :emitter emitter-position)]
    (j/call ps :createSphereEmitter 0.1)
    ;; TODO when on dispose remove before-render-fn
    (api.core/register-before-render-fn
      before-render-fn
      (fn []
        (let [speed-factor 4
              elapsed (* speed-factor (api.core/get-elapsed-time))
              r 1
              x (+ (j/get position :x) (* r (Math/cos elapsed)))
              y (+ (j/get position :y)
                   (Math/sin (/ elapsed 5))
                   ;; (Math/abs (Math/sin (/ elapsed 5)))
                   )
              z (+ (j/get position :z) (* r (Math/sin elapsed)))]
          (j/assoc! emitter-position :x x)
          (j/assoc! emitter-position :y y)
          (j/assoc! emitter-position :z z))))
    ps))

(comment
  (let [origin (v3 0 0 0)
        position (v3 0 0 0)
        p (create-particle-system "p"
                                  :particle-texture (api.core/texture "img/texture/star_04.png")
                                  :capacity 2000
                                  :min-life-time 0.3
                                  :max-life-time 0.5
                                  :color1 (api.core/color 0.7 0.8 1.0 1.0)
                                  :color2 (api.core/color 0.2 0.5 1.0 1.0)
                                  :color-dead (api.core/color 0 0 0.2 0.0)
                                  :min-size 0.1
                                  :max-size 0.3
                                  :gravity (v3 0 -9.81 0)
                                  :min-angular-speed 0
                                  :max-angular-speed Math/PI
                                  :min-emit-power 1
                                  :max-emit-power 3
                                  :update-speed 0.005
                                  :emit-rate 1000
                                  :target-stop-duration 2
                                  :emitter position)]
    (j/call p :createSphereEmitter 0.1)
    (start p)
    (api.core/register-before-render-fn
      "box-around"
      (fn []
        (let [speed-factor 4
              elapsed (* speed-factor (api.core/get-elapsed-time))
              r 1
              x (+ (j/get origin :x) (* r (Math/cos elapsed)))
              y (+ (j/get origin :y)
                   (Math/sin (/ elapsed 5))
                   ;(Math/abs (Math/sin (/ elapsed 5)))
                   )
              z (+ (j/get origin :z) (* r (Math/sin elapsed)))]
          (j/assoc! position :x x)
          (j/assoc! position :y y)
          (j/assoc! position :z z)
          )
        ))

    )

  (stop p)
  (reset p)
  )
