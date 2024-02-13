(ns immersa.scene.api.particle
  (:require
    ["@babylonjs/core/Particles/gpuParticleSystem" :refer [GPUParticleSystem]]
    ["@babylonjs/core/Particles/particleSystem" :refer [ParticleSystem]]
    [applied-science.js-interop :as j]
    [immersa.scene.api.constant :as api.const]
    [immersa.scene.api.core :as api.core :refer [v3]]
    [immersa.scene.api.mesh :as api.mesh])
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
                                             position
                                             rotation
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
                                             limit-velocity-damping
                                             animation-sheet-enabled?
                                             sprite-cell-height
                                             sprite-cell-width
                                             start-sprite-cell-id
                                             end-sprite-cell-id
                                             sprite-cell-change-speed
                                             sprite-cell-loop?
                                             sprite-random-start-cell?]
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
      position (j/assoc! :position position)
      rotation (j/assoc! :rotation rotation)
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
      sprite-cell-height (j/assoc! :spriteCellHeight sprite-cell-height)
      sprite-cell-width (j/assoc! :spriteCellWidth sprite-cell-width)
      start-sprite-cell-id (j/assoc! :startSpriteCellID start-sprite-cell-id)
      end-sprite-cell-id (j/assoc! :endSpriteCellID end-sprite-cell-id)
      sprite-cell-change-speed (j/assoc! :spriteCellChangeSpeed sprite-cell-change-speed)
      (some? sprite-cell-loop?) (j/assoc! :spriteCellLoop sprite-cell-loop?)
      (some? sprite-random-start-cell?) (j/assoc! :spriteRandomStartCell sprite-random-start-cell?)
      (some? local?) (j/assoc! :isLocal local?)
      target-stop-duration (j/assoc! :targetStopDuration (if update-speed
                                                           (/ target-stop-duration (/ 0.01 update-speed))
                                                           target-stop-duration))
      (some? animation-sheet-enabled?) (j/assoc! :isAnimationSheetEnabled animation-sheet-enabled?)
      (some? dispose-on-stop?) (j/assoc! :disposeOnStop dispose-on-stop?)
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

(defn sparkles [name & {:keys [position
                               radius
                               target-stop-duration
                               speed-factor]
                        :or {position (v3 0 0 0)
                             radius 1
                             speed-factor 4}}]
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
                                   :emitter emitter-position
                                   :position position)]
    (j/call ps :createSphereEmitter 0.1)
    ;; TODO when on dispose remove before-render-fn
    (api.core/register-before-render-fn
      before-render-fn
      (fn []
        (let [elapsed (* speed-factor (api.core/get-elapsed-time))
              position (j/get ps :position)
              x (+ (j/get position :x) #_(* radius (Math/cos elapsed)))
              y (+ (j/get position :y)
                   #_(Math/sin (/ elapsed 5))
                   ;; (Math/abs (Math/sin (/ elapsed 5)))
                   )
              z (+ (j/get position :z) #_(* radius (Math/sin elapsed)))]
          (j/assoc! emitter-position :x x)
          (j/assoc! emitter-position :y y)
          (j/assoc! emitter-position :z z))))
    ps))

(defn clouds [name & {:keys [position
                             rotation
                             scale]
                      :or {position (v3)
                           rotation (v3)
                           scale (v3 0.1)}}]
  (let [cloud-mesh (api.mesh/sphere (str name "-mesh")
                                    :position position
                                    :rotation rotation
                                    :diameter 0.1
                                    :segments 16
                                    :scale scale
                                    :visible? false)
        scale-factor (j/get scale :x)
        before-render-fn (str name "-clouds-before-render")
        cloud-system (create-particle-system name
                                             :position position
                                             :rotation rotation
                                             :capacity 750
                                             :particle-texture (api.core/texture "img/texture/clouds.png")
                                             :blend-mode api.const/particle-blend-mode-standard
                                             :animation-sheet-enabled? true
                                             :sprite-cell-height 512
                                             :sprite-cell-width 512
                                             :start-sprite-cell-id 3
                                             :end-sprite-cell-id 15
                                             :sprite-cell-change-speed 0
                                             :sprite-cell-loop? false
                                             :sprite-random-start-cell? true
                                             :min-emit-box (v3 -1 0 0)
                                             :max-emit-box (v3 1 0 0)
                                             :min-life-time 1
                                             :max-life-time 1
                                             :emit-rate 500
                                             :gravity (v3 0 -0.8 0)
                                             :direction1 (v3 -2 8 2)
                                             :direction2 (v3 2 8 -2)
                                             :min-angular-speed 0
                                             :max-angular-speed 0.1
                                             :min-emit-power 0.04
                                             :max-emit-power 0.06
                                             :update-speed 0.01
                                             :pre-warm-step-offset 10
                                             :pre-warm-cycles 100
                                             :emitter cloud-mesh
                                             ;; :local? true
                                             :min-size (* 0.3 scale-factor)
                                             :max-size (* 0.3 scale-factor)
                                             :color-gradients [[0 (api.core/color 1 1 1 0)]
                                                               [0.3 (api.core/color 1 1 1 0.7)]
                                                               [1 (api.core/color 0.3 0.3 0.3 0)]])]
    (api.core/register-before-render-fn
      before-render-fn
      (fn []
        (let [position (j/get cloud-system :position)
              x (j/get position :x)
              y (j/get position :y)
              z (j/get position :z)]
          (j/assoc-in! cloud-mesh [:position :x] x)
          (j/assoc-in! cloud-mesh [:position :y] y)
          (j/assoc-in! cloud-mesh [:position :z] z))))
    (j/call cloud-system :createCylinderEmitter 1 0.1)
    cloud-system))

(comment
  (j/get (api.core/get-object-by-name "cloud-particle") :position)
  (api.core/dispose "cloud-particle")
  (start
    (clouds "cloud-particle"
            :scale (v3 0.9)
            :update-speed 0.01
            :position (v3 0 1.1 3)))

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
