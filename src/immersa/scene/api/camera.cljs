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
