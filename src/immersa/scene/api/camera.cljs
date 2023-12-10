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
    (api.core/add-node-to-db name camera (assoc opts :type :free
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
                                        radius
                                        target-screen-offset
                                        use-bouncing-behavior?
                                        apply-gravity?
                                        collision-radius
                                        lower-radius-limit
                                        upper-radius-limit
                                        min-z]
                                 :or {min-z 0.1}
                                 :as opts}]
  (let [camera (ArcRotateCamera. name 0 0 0 (v3))
        init-rotation (clone (j/get camera :rotation))
        init-position (clone (j/get camera :position))]
    (api.core/add-node-to-db name camera (assoc opts :type :arc
                                                :init-rotation init-rotation
                                                :init-position init-position))
    (m/cond-doto camera
      position (j/call :setPosition position)
      target (j/call :setTarget target))
    (j/call camera :attachControl canvas true)
    (j/assoc! camera
              :radius radius
              :targetScreenOffset target-screen-offset
              :useBouncingBehavior use-bouncing-behavior?
              :applyGravity apply-gravity?
              :collisionRadius collision-radius
              :lowerRadiusLimit lower-radius-limit
              :upperRadiusLimit upper-radius-limit
              :init-rotation init-rotation
              :init-position init-position
              :minZ min-z
              :type :arc)
    camera))
