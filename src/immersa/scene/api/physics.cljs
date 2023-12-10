(ns immersa.scene.api.physics
  (:require
    ["@babylonjs/core/Physics/physicsRaycastResult" :refer [PhysicsRaycastResult]]
    ["@babylonjs/core/Physics/v2/IPhysicsEnginePlugin" :refer [PhysicsMotionType PhysicsShapeType]]
    ["@babylonjs/core/Physics/v2/physicsAggregate" :refer [PhysicsAggregate]]
    [applied-science.js-interop :as j])
  (:require-macros
    [immersa.scene.macros :as m]))

(defn physics-agg [mesh & {:keys [type
                                  mass
                                  friction
                                  restitution
                                  motion-type
                                  mass-props
                                  linear-damping
                                  angular-damping
                                  gravity-factor]}]
  (let [agg (PhysicsAggregate. mesh (j/get PhysicsShapeType (name type)) #js {:mass mass
                                                                              :friction friction
                                                                              :restitution restitution})]
    (m/cond-doto agg
      gravity-factor (j/call-in [:body :setGravityFactor] gravity-factor)
      linear-damping (j/call-in [:body :setLinearDamping] linear-damping)
      angular-damping (j/call-in [:body :setAngularDamping] angular-damping)
      mass-props (j/call-in [:body :setMassProperties] (clj->js mass-props))
      motion-type (j/call-in [:body :setMotionType] (j/get PhysicsMotionType (name motion-type))))))

(defn raycast-result []
  (PhysicsRaycastResult.))

(defn raycast-to-ref [p1 p2 result]
  (let [scene (j/get db :scene)
        physics-engine (j/call scene :getPhysicsEngine)]
    (j/call physics-engine :raycastToRef p1 p2 result)
    result))

(defn apply-force [mesh force location]
  (j/call-in mesh [:physicsBody :applyForce] force location))

(defn apply-impulse [mesh impulse location]
  (j/call-in mesh [:physicsBody :applyImpulse] impulse location))

(defn get-linear-velocity-to-ref [mesh ref]
  (j/call-in mesh [:physicsBody :getLinearVelocityToRef] ref))

(defn set-linear-velocity [mesh dir]
  (j/call-in mesh [:physicsBody :setLinearVelocity] dir))

(defn get-object-center-world [mesh]
  (j/call-in mesh [:physicsBody :getObjectCenterWorld]))
