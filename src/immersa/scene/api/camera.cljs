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

(defn detach-control [camera]
  (j/call camera :detachControl))

(defn set-pos [pos]
  (j/call-in (active-camera) [:position :copyFrom] pos))

(defn reset-camera []
  (let [cam (active-camera)]
    (j/assoc! cam
              :position (api.core/clone (j/get cam :init-position))
              :rotation (api.core/clone (j/get cam :init-rotation)))))

(defn create-free-camera [name & {:keys [position speed min-z]
                                  :or {position (v3 0 2 -10)
                                       speed 0.5
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
                                        min-z]
                                 :or {min-z 0.1
                                      alpha (/ Math/PI 2)
                                      beta (/ Math/PI 2)
                                      radius 10
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
      true (j/assoc! :type :arc)
      true (j/assoc! :init-rotation init-rotation)
      true (j/assoc! :init-position init-position))))

(defn calculate-new-fov [original-fov frame-height container-height]
  (* 2 (Math/atan (* (Math/tan (/ original-fov 2)) (/ container-height frame-height)))))

(defn switch-camera-if-needed [scene]
  (let [wasd? (boolean (seq (filter true? (map #(j/get-in api.core/db [:keyboard %]) ["w" "a" "s" "d" "e" "q"]))))
        switch-type (cond
                      wasd? :free
                      (api.core/selected-mesh) :arc
                      :else :free)
        camera (active-camera)
        camera-type (j/get camera :type)
        arc-camera (api.core/get-object-by-name "arc-camera")
        free-camera (api.core/get-object-by-name "free-camera")
        canvas (api.core/canvas)]
    (cond
      (and (= switch-type :free) (not (= camera-type :free)))
      (let [position (api.core/clone (j/get arc-camera :position))
            target (j/call arc-camera :getTarget)]
        (j/call-in free-camera [:position :copyFrom] position)
        (j/call free-camera :setTarget (api.core/clone target))
        (j/assoc! scene :activeCamera free-camera)
        (detach-control arc-camera)
        (j/call free-camera :attachControl canvas true))

      (and (= switch-type :arc) (not (= camera-type :arc)))
      (let [position (api.core/clone (j/get free-camera :position))
            target (j/call free-camera :getTarget)]
        (api.core/set-pos arc-camera position)
        (j/call arc-camera :setTarget (api.core/clone target))
        (j/assoc! scene :activeCamera arc-camera)
        (detach-control free-camera)
        (j/call arc-camera :attachControl canvas true)))))

;; 931 522
;; 1026 576
(comment
  (j/get (active-camera) :fov)
  (j/assoc! (active-camera) :fov 0.8)
  (j/assoc! (active-camera) :fov 1.1578)
  (calculate-new-fov 0.8 500 773)
  (calculate-new-fov 0.8730305915925877 576 522)
  )
