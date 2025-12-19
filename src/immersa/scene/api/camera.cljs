(ns immersa.scene.api.camera
  (:require
    ["@babylonjs/core/Cameras/arcRotateCamera" :refer [ArcRotateCamera]]
    ["@babylonjs/core/Cameras/freeCamera" :refer [FreeCamera]]
    [applied-science.js-interop :as j]
    [immersa.scene.api.core :as api.core :refer [db v3]])
  (:require-macros
    [immersa.scene.macros :as m]))

(defn active-camera []
  (j/get-in db [:scene :activeCamera]))

(defn update-active-camera []
  (j/assoc-in! db [:nodes :camera :obj] (active-camera)))

(defn attach-control [camera]
  (j/call camera :attachControl (api.core/get-canvas) true))

(defn detach-control [camera]
  (j/call camera :detachControl))

(defn disable-cameras []
  (let [free-camera (api.core/get-object-by-name "free-camera")
        arc-camera (api.core/get-object-by-name "arc-camera")]
    (detach-control free-camera)
    (detach-control arc-camera)))

(defn enable-cameras []
  (let [free-camera (api.core/get-object-by-name "free-camera")
        arc-camera (api.core/get-object-by-name "arc-camera")]
    (attach-control free-camera)
    (attach-control arc-camera)))

(defn set-pos [pos]
  (j/call-in (active-camera) [:position :copyFrom] pos))

(defn reset-camera [position rotation]
  (let [cam (active-camera)]
    (j/assoc! cam
              :position (api.core/clone position)
              :rotation (api.core/clone rotation))))

(defn create-free-camera [name & {:keys [position speed min-z]
                                  :or {position (v3 0 2 -10)
                                       speed 0.1
                                       min-z 0.1}
                                  :as opts}]
  (let [camera (FreeCamera. name position)
        init-rotation (api.core/clone (j/get camera :rotation))
        init-position (api.core/clone (j/get camera :position))]
    (api.core/add-node-to-db name camera (assoc opts
                                                :type :free
                                                :init-rotation init-rotation
                                                :init-position init-position))
    (j/call-in camera [:keysUpward :push] 69)
    (j/call-in camera [:keysDownward :push] 81)
    (j/call-in camera [:keysUp :push] 87)
    (j/call-in camera [:keysDown :push] 83)
    (j/call-in camera [:keysLeft :push] 65)
    (j/call-in camera [:keysRight :push] 68)
    (j/assoc! camera
              :speed speed
              :type :free
              :minZ min-z
              :init-rotation init-rotation
              :init-position init-position)
    camera))

(defn create-arc-camera [name & {:keys [canvas
                                        target
                                        position
                                        alpha
                                        beta
                                        radius
                                        target-screen-offset
                                        use-bouncing-behavior?
                                        apply-gravity?
                                        collision-radius
                                        lower-radius-limit
                                        upper-radius-limit
                                        angular-sensibility-x
                                        angular-sensibility-y
                                        wheel-precision
                                        speed
                                        min-z]
                                 :or {min-z 0.1
                                      alpha (/ Math/PI 2)
                                      beta (/ Math/PI 2)
                                      lower-radius-limit 0.1
                                      angular-sensibility-x 2000
                                      angular-sensibility-y 2000
                                      wheel-precision 50
                                      radius 10
                                      speed 0.1
                                      target (v3)}
                                 :as opts}]
  (let [camera (ArcRotateCamera. name alpha beta radius target)
        init-rotation (api.core/clone (j/get camera :rotation))
        init-position (api.core/clone (j/get camera :position))]
    (api.core/add-node-to-db name camera (assoc opts
                                                :type :arc
                                                :init-rotation init-rotation
                                                :init-position init-position))
    (j/call camera :attachControl canvas true)
    (m/cond-doto camera
      camera (j/call :setPosition position)
      target-screen-offset (j/assoc! :setTargetScreenOffset target-screen-offset)
      (some? use-bouncing-behavior?) (j/assoc! :useBouncingBehavior use-bouncing-behavior?)
      (some? apply-gravity?) (j/assoc! :applyGravity apply-gravity?)
      collision-radius (j/assoc! :collisionRadius collision-radius)
      lower-radius-limit (j/assoc! :lowerRadiusLimit lower-radius-limit)
      upper-radius-limit (j/assoc! :upperRadiusLimit upper-radius-limit)
      min-z (j/assoc! :minZ min-z)
      speed (j/assoc! :speed speed)
      wheel-precision (j/assoc! :wheelPrecision wheel-precision)
      angular-sensibility-x (j/assoc! :angularSensibilityX angular-sensibility-x)
      angular-sensibility-y (j/assoc! :angularSensibilityY angular-sensibility-y)
      true (j/assoc! :type :arc)
      true (j/assoc! :init-rotation init-rotation)
      true (j/assoc! :init-position init-position))))

(defn calculate-new-fov [original-fov frame-height container-height]
  (* 2 (Math/atan (* (Math/tan (/ original-fov 2)) (/ container-height frame-height)))))

(defn toggle-camera-lock [locked?]
  (if locked?
    (disable-cameras)
    (enable-cameras)))

(defn switch-camera-if-needed [locked?]
  (when-not locked?
    (let [scene (api.core/get-scene)
          wasd? (boolean (seq (filter true? (map #(j/get-in api.core/db [:keyboard %]) ["w" "a" "s" "d" "e" "q"]))))
          switch-type (cond
                        wasd? :free
                        (api.core/selected-mesh) :arc
                        (j/get-in api.core/db [:mouse :left-click?]) :arc
                        (j/get-in api.core/db [:mouse :right-click?]) :arc
                        (j/get-in api.core/db [:mouse :wheel?]) :arc
                        :else :free)
          camera (active-camera)
          camera-type (j/get camera :type)
          arc-camera (api.core/get-object-by-name "arc-camera")
          free-camera (api.core/get-object-by-name "free-camera")]
      (cond
        (and (= switch-type :free) (not (= camera-type :free)))
        (let [position (api.core/clone (j/get arc-camera :position))
              target (j/call arc-camera :getTarget)]
          (j/call-in free-camera [:position :copyFrom] position)
          (j/call free-camera :setTarget (api.core/clone target))
          (j/assoc! scene :activeCamera free-camera)
          (detach-control arc-camera)
          (attach-control free-camera))

        (and (= switch-type :arc) (not (= camera-type :arc)))
        (let [position (api.core/clone (j/get free-camera :position))
              target (j/call free-camera :getTarget)]
          (api.core/set-pos arc-camera position)
          (j/call arc-camera :setTarget (api.core/clone target))
          (j/assoc! scene :activeCamera arc-camera)
          (detach-control free-camera)
          (attach-control arc-camera))))))

;; 931 522
;; 1026 576
(comment
  ;canvas.toDataURL('image/webp', 0.5);
  (j/call-in api.core/db [:canvas :toDataURL] "image/webp" 0.2)
  (create-screenshot (fn [s] (println "S: " s)))
  (j/get (active-camera) :fov)
  (j/assoc! (active-camera) :fov 0.8)
  (j/assoc! (active-camera) :fov 1.1578)
  (calculate-new-fov 0.8 500 773)
  (calculate-new-fov 0.8730305915925877 576 522)
  )
