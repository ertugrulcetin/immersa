(ns immersa.scene.api
  (:require
    ["@babylonjs/core/Engines/engine" :refer [Engine]]
    ["@babylonjs/core/scene" :refer [Scene]]
    ["@babylonjs/core/Actions/actionManager" :refer [ActionManager]]
    ["@babylonjs/core/Actions/directActions":refer [ExecuteCodeAction]]
    ["@babylonjs/core/Maths/math" :refer [Vector2 Vector3]]
    ["@babylonjs/core/Maths/math.color" :refer [Color3]]
    ["@babylonjs/core/Misc/tools" :refer [Tools]]
    ["@babylonjs/core/Cameras/freeCamera" :refer [FreeCamera]]
    ["@babylonjs/core/Cameras/arcRotateCamera" :refer [ArcRotateCamera]]
    ["@babylonjs/core/Lights/hemisphericLight" :refer [HemisphericLight]]
    ["@babylonjs/core/Lights/directionalLight" :refer [DirectionalLight]]
    ["@babylonjs/core/Lights/Shadows/shadowGenerator" :refer [ShadowGenerator]]
    ["@babylonjs/core/Meshes/meshBuilder" :refer [MeshBuilder]]
    ["@babylonjs/core/Materials/standardMaterial" :refer [StandardMaterial]]
    ["@babylonjs/gui/2D" :refer [AdvancedDynamicTexture]]
    ["@babylonjs/gui/2D/controls" :refer [Button Image]]
    ["@babylonjs/core/Loading/sceneLoader" :refer [SceneLoader]]
    ["@babylonjs/core/Materials/Textures/texture" :refer [Texture]]
    ["@babylonjs/core/Materials/Textures/cubeTexture" :refer [CubeTexture]]
    ["@babylonjs/core/Physics/physicsRaycastResult" :refer [PhysicsRaycastResult]]
    ["@babylonjs/core/Physics/v2/IPhysicsEnginePlugin" :refer [PhysicsMotionType PhysicsShapeType]]
    ["@babylonjs/core/Physics/v2/physicsAggregate" :refer [PhysicsAggregate]]
    [applied-science.js-interop :as j])
  (:require-macros
    [immersa.scene.macros :as m]))

(defonce db #js {})

(def v3-up (j/call Vector3 :Up))
(def v3-forward (j/call Vector3 :Forward))
(def v3-left (j/call Vector3 :Left))
(def v3-right (j/call Vector3 :Right))

(defn create-engine [canvas]
  (let [e (Engine. canvas true #js {:preserveDrawingBuffer true
                                    :stencil true})]
    (j/assoc! db :engine e :canvas canvas)
    e))

(defn create-scene [engine]
  (let [s (Scene. engine)]
    (j/assoc! db :scene s)
    s))

(defn v3
  ([]
   (v3 0))
  ([n]
   (Vector3. n n n))
  ([x y z]
   (Vector3. x y z)))

(defn v2
  ([]
   (v2 0))
  ([n]
   (Vector2. n n))
  ([x z]
   (Vector2. x z)))

(defn set-v3 [v x y z]
  (j/call v :set x y z))

(defn get-delta-time []
  (/ (j/call-in db [:engine :getDeltaTime]) 1000))

(defn color
  ([c]
   (color c c c))
  ([r g b]
   (Color3. r g b)))

(defn color-rgb
  ([c]
   (color-rgb c c c))
  ([r g b]
   (j/call Color3 :FromInts r g b)))

(defn box [name & {:keys [size]}]
  (j/call MeshBuilder :CreateBox name #js {:size size}))

(defn capsule [name & {:keys [height radius]}]
  (j/call MeshBuilder :CreateCapsule name #js {:height height
                                               :radius radius}))

(defn get-pos [mesh]
  (j/call mesh :getAbsolutePosition))

(defn create-action-manager [obj]
  (let [am (ActionManager.)]
    (j/assoc! obj :actionManager am)
    am))

(defn register-action [action-manager params callback]
  (j/call action-manager :registerAction (ExecuteCodeAction.
                                           (if (keyword? params)
                                             (j/get ActionManager (name params))
                                             (clj->js (update params :trigger #(j/get ActionManager (name %)))))
                                           callback)))

(defn create-ground [name & {:keys [width height]}]
  (j/call MeshBuilder :CreateGround name #js {:width width
                                              :height height}))

(defn create-ground-from-hm [name & {:keys [texture subdivisions width height max-height min-height on-ready]}]
  (j/call MeshBuilder :CreateGroundFromHeightMap name texture #js {:subdivisions subdivisions
                                                                   :width width
                                                                   :height height
                                                                   :maxHeight max-height
                                                                   :minHeight min-height
                                                                   :onReady on-ready}))

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

(defn standard-mat [name & {:keys [diffuse-texture
                                   diffuse-color
                                   specular-color
                                   back-face-culling?
                                   reflection-texture
                                   coordinates-mode
                                   disable-lighting?
                                   emissive-color]}]
  (cond-> (StandardMaterial. name)
          diffuse-texture (j/assoc! :diffuseTexture diffuse-texture)
          specular-color (j/assoc! :specularColor specular-color)
          (some? back-face-culling?) (j/assoc! :backFaceCulling back-face-culling?)
          reflection-texture (j/assoc! :reflectionTexture reflection-texture)
          coordinates-mode (j/assoc-in! [:reflectionTexture :coordinatesMode] (j/get Texture coordinates-mode))
          (some? disable-lighting?) (j/assoc! :disableLighting disable-lighting?)
          diffuse-color (j/assoc! :diffuseColor diffuse-color)
          emissive-color (j/assoc! :emissiveColor emissive-color)))

(defn create-sky-box []
  (let [skybox (box "skyBox" :size 5000.0)
        mat (standard-mat "skyBox"
                          :back-face-culling? false
                          :reflection-texture (CubeTexture. "" nil nil nil #js ["img/skybox/px.jpeg"
                                                                                "img/skybox/py.jpeg"
                                                                                "img/skybox/pz.jpeg"
                                                                                "img/skybox/nx.jpeg"
                                                                                "img/skybox/ny.jpeg"
                                                                                "img/skybox/nz.jpeg"])
                          :coordinates-mode :SKYBOX_MODE
                          :diffuse-color (color 0 0 0)
                          :specular-color (color 0 0 0)
                          :disable-lighting? true)]
    (j/assoc! skybox :material mat)
    skybox))

(defn texture [path & {:keys [u-scale v-scale]}]
  (let [tex (Texture. path)]
    (m/cond-doto tex
                 u-scale (j/assoc! :uScale u-scale)
                 v-scale (j/assoc! :vScale v-scale))))

(defn key-pressed? [key]
  (j/get-in db [:input-map key] false))

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

(defn directional-light [name & {:keys [dir pos]}]
  (let [light (DirectionalLight. name dir)]
    (j/assoc! light :position pos)))

(defn shadow-generator [& {:keys [map-size light]
                           :or {map-size 1024}}]
  (ShadowGenerator. map-size light))

(defn add-shadow-caster [shadow-generator mesh]
  (j/call shadow-generator :addShadowCaster mesh))

(defn register-event-listener [element type f]
  (j/call element :addEventListener type f)
  (j/update! db :event-listeners (fnil conj []) [element type f])
  f)

(defn remove-element-listeners []
  (doseq [[element type f] (m/get db :event-listeners)]
    (j/call element :removeEventListener type f))
  (j/assoc! db :event-listeners [])
  (println "removed event listeners"))

(defn create-arc-camera [name & {:keys [canvas
                                        player
                                        pos
                                        radius
                                        target-screen-offset
                                        use-bouncing-behavior?
                                        apply-gravity?
                                        collision-radius
                                        lower-radius-limit
                                        upper-radius-limit]}]
  (let [camera (ArcRotateCamera. name 0 0 10 (v3))]
    (doto camera
      (j/call :setPosition pos)
      (j/call :attachControl canvas true)
      (j/call :setTarget player))
    (j/assoc! camera
              :radius radius
              :targetScreenOffset target-screen-offset
              :useBouncingBehavior use-bouncing-behavior?
              :applyGravity apply-gravity?
              :collisionRadius collision-radius
              :lowerRadiusLimit lower-radius-limit
              :upperRadiusLimit upper-radius-limit)
    (j/assoc! db :camera camera)
    camera))

(defn raycast-result []
  (PhysicsRaycastResult.))

(defn raycast-to-ref [p1 p2 result]
  (let [scene (j/get db :scene)
        physics-engine (j/call scene :getPhysicsEngine)]
    (j/call physics-engine :raycastToRef p1 p2 result)
    result))

(defn register-on-before-render [f]
  (j/call-in db [:scene :onBeforeRenderObservable :add] f))

(defn register-on-after-render [f]
  (j/call-in db [:scene :onAfterRenderObservable :add] f))

(defn advanced-dynamic-texture []
  (let [gui-texture (j/call-in AdvancedDynamicTexture [:CreateFullscreenUI] "UI")]
    (j/assoc! db :gui-texture gui-texture)
    gui-texture))

(defn gui-image [name url]
  (Image. name url))

(defn gui-button [name text]
  (j/call Button :CreateSimpleButton name text))

(defn add-control [texture control]
  (j/call texture :addControl control))

(defn import-mesh [file f]
  (j/call SceneLoader :ImportMesh "" "models/" file (j/get db :scene) f))

(defn normalize [v]
  (j/call v :normalize))

(defn scale [v n]
  (j/call v :scale n))

(defn to-rad [angle]
  (j/call Tools :ToRadians angle))

(defn to-deg [angle]
  (j/call Tools :ToDegrees angle))
