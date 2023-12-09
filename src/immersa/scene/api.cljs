(ns immersa.scene.api
  (:refer-clojure :exclude [clone])
  (:require
    ["@babylonjs/core/Actions/actionManager" :refer [ActionManager]]
    ["@babylonjs/core/Actions/directActions" :refer [ExecuteCodeAction]]
    ["@babylonjs/core/Animations/animatable"]
    ["@babylonjs/core/Animations/animation" :refer [Animation]]
    ["@babylonjs/core/Animations/easing" :refer [CubicEase EasingFunction]]
    ["@babylonjs/core/Cameras/arcRotateCamera" :refer [ArcRotateCamera]]
    ["@babylonjs/core/Cameras/freeCamera" :refer [FreeCamera]]
    ["@babylonjs/core/Debug/debugLayer"]
    ["@babylonjs/core/Engines/engine" :refer [Engine]]
    ["@babylonjs/core/Layers/glowLayer" :refer [GlowLayer]]
    ["@babylonjs/core/Layers/highlightLayer" :refer [HighlightLayer]]
    ["@babylonjs/core/Lights/Shadows/shadowGenerator" :refer [ShadowGenerator]]
    ["@babylonjs/core/Lights/directionalLight" :refer [DirectionalLight]]
    ["@babylonjs/core/Lights/hemisphericLight" :refer [HemisphericLight]]
    ["@babylonjs/core/Loading/sceneLoader" :refer [SceneLoader]]
    ["@babylonjs/core/Materials/Textures/cubeTexture" :refer [CubeTexture]]
    ["@babylonjs/core/Materials/Textures/dynamicTexture" :refer [DynamicTexture]]
    ["@babylonjs/core/Materials/Textures/texture" :refer [Texture]]
    ["@babylonjs/core/Materials/standardMaterial" :refer [StandardMaterial]]
    ["@babylonjs/core/Maths/math" :refer [Vector2 Vector3 Vector4]]
    ["@babylonjs/core/Maths/math.color" :refer [Color3]]
    ["@babylonjs/core/Meshes/mesh" :refer [Mesh]]
    ["@babylonjs/core/Meshes/meshBuilder" :refer [MeshBuilder]]
    ["@babylonjs/core/Meshes/transformNode" :refer [TransformNode]]
    ["@babylonjs/core/Misc/tools" :refer [Tools]]
    ["@babylonjs/core/Physics/physicsRaycastResult" :refer [PhysicsRaycastResult]]
    ["@babylonjs/core/Physics/v2/IPhysicsEnginePlugin" :refer [PhysicsMotionType PhysicsShapeType]]
    ["@babylonjs/core/Physics/v2/physicsAggregate" :refer [PhysicsAggregate]]
    ["@babylonjs/core/scene" :refer [Scene]]
    ["@babylonjs/gui/2D" :refer [AdvancedDynamicTexture Control TextWrapping]]
    ["@babylonjs/gui/2D/controls" :refer [Button Image Rectangle TextBlock]]
    ["@babylonjs/inspector"]
    ["@babylonjs/materials/grid/gridMaterial" :refer [GridMaterial]]
    ["earcut" :as earcut]
    [applied-science.js-interop :as j]
    [cljs.core.async :as a :refer [go <!]]
    [immersa.scene.font :as font])
  (:require-macros
    [immersa.scene.macros :as m]))

(defonce db #js {})

(def v3-up (j/call Vector3 :Up))
(def v3-forward (j/call Vector3 :Forward))
(def v3-left (j/call Vector3 :Left))
(def v3-right (j/call Vector3 :Right))

(def animation-type-v3 :ANIMATIONTYPE_VECTOR3)
(def animation-type-float :ANIMATIONTYPE_FLOAT)
(def animation-loop-cons :ANIMATIONLOOPMODE_CONSTANT)
(def animation-loop-cycle :ANIMATIONLOOPMODE_CYCLE)

(def easing-ease-in :EASINGMODE_EASEIN)
(def easing-ease-out :EASINGMODE_EASEOUT)
(def easing-ease-in-out :EASINGMODE_EASEINOUT)

(def gui-horizontal-align-left :HORIZONTAL_ALIGNMENT_LEFT)
(def gui-horizontal-align-center :HORIZONTAL_ALIGNMENT_CENTER)
(def gui-vertical-align-center :VERTICAL_ALIGNMENT_CENTER)

(def gui-text-wrapping-word-wrap :WordWrap)

(def mesh-billboard-mode-all :BILLBOARDMODE_ALL)

(def color-white (j/call Color3 :White))
(def color-black (j/call Color3 :Black))
(def color-yellow (j/call Color3 :Yellow))

(defn create-engine [canvas]
  (let [e (Engine. canvas true #js {:preserveDrawingBuffer true
                                    :stencil true
                                    :adaptToDeviceRatio true})]
    (j/assoc! db :engine e :canvas canvas)
    (j/assoc! js/window :engine e)
    e))

(defn create-scene [engine]
  (let [s (Scene. engine)]
    (j/assoc! db :scene s)
    s))

(defn canvas []
  (j/get db :canvas))

(defn v2
  ([]
   (Vector2.))
  ([n]
   (Vector2. n n))
  ([x z]
   (Vector2. x z)))

(defn v3
  ([]
   (Vector3.))
  ([n]
   (Vector3. n n n))
  ([x y z]
   (Vector3. x y z)))

(defn v4
  ([]
   (Vector4.))
  ([n]
   (Vector4. n n n n))
  ([x y z w]
   (Vector4. x y z w)))

(defn clone [obj]
  (j/call obj :clone))

(defn set-v3 [v x y z]
  (j/call v :set x y z))

(defn equals? [v1 v2]
  (j/call v1 :equals v2))

(defn get-delta-time []
  (/ (j/call-in db [:engine :getDeltaTime]) 1000))

(defn color
  ([c]
   (color c c c))
  ([r g b]
   (Color3. r g b 1.0))
  ([r g b a]
   (Color3. r g b a)))

(defn color-rgb
  ([c]
   (color-rgb c c c))
  ([r g b]
   (j/call Color3 :FromInts r g b)))

(defn scaling
  ([m n]
   (scaling m n n n))
  ([m x y z]
   (j/assoc! m :scaling (v3 x y z))))

(defn get-node-by-name [name]
  (j/get-in db [:nodes name]))

(defn get-object-by-name [name]
  (j/get-in db [:nodes name :obj]))

(defn get-objects-by-type [type]
  (map (j/get :obj) (filter #(= (j/get % :type) type) (js/Object.values (j/get db :nodes)))))

(defn dispose [& name-or-obj]
  (doseq [o name-or-obj]
    (when-let [obj (if (string? o)
                     (get-object-by-name o)
                     o)]
      (let [obj-name (j/get obj :name)
            children (j/get-in db [:nodes obj-name :children])]
        (j/call obj :dispose)
        (js-delete (j/get db :nodes) obj-name)
        (doseq [c children]
          (dispose c))))))

(defn dispose-all [objs]
  (apply dispose objs))

(defn- check-name-exists [name obj]
  (when (j/get-in db [:nodes name])
    (dispose obj)
    (throw (js/Error. (str "Node with name " name " already exists")))))

(defn add-node-to-db [name obj opts]
  (check-name-exists name obj)
  (j/assoc-in! db [:nodes name] (clj->js (assoc opts :obj obj :name name)))
  obj)

(defn add-prop-to-db [name prop val]
  (j/assoc-in! db [:nodes name prop] val))

(defn box [name & {:keys [size
                          width
                          height
                          depth
                          face-uv
                          wrap?
                          visibility
                          position
                          skybox?
                          infinite-distance?
                          mat]
                   :as opts}]
  (let [b (j/call MeshBuilder :CreateBox name #js {:size size
                                                   :faceUV face-uv
                                                   :wrap wrap?
                                                   :width width
                                                   :height height
                                                   :depth depth})]
    (add-node-to-db name b (assoc opts :type (if skybox?
                                               :skybox
                                               :box)))
    (cond-> b
      mat (j/assoc! :material mat)
      position (j/assoc! :position position)
      visibility (j/assoc! :visibility visibility)
      (some? infinite-distance?) (j/assoc! :infiniteDistance infinite-distance?))))

(defn sphere [name & {:keys [diameter
                             segments
                             updatable?
                             arc
                             slice
                             visibility
                             position
                             rotation
                             scale
                             mat]
                      :or {segments 32
                           diameter 1
                           arc 1
                           slice 1
                           updatable? false}
                      :as opts}]
  (let [s (j/call MeshBuilder :CreateSphere name #js {:diameter diameter
                                                      :segments segments
                                                      :arc arc
                                                      :slice slice
                                                      :updatable updatable?})]
    (add-node-to-db name s opts)
    (m/cond-doto s
      mat (j/assoc! :material mat)
      position (j/assoc! :position position)
      rotation (j/assoc! :rotation rotation)
      scale (scaling scale)
      visibility (j/assoc! :visibility visibility))))

(defn capsule [name & {:keys [height radius visibility]
                       :as opts}]
  (let [c (j/call MeshBuilder :CreateCapsule name #js {:height height
                                                       :radius radius})]
    (add-node-to-db name c opts)
    (cond-> c
      visibility (j/assoc! :visibility visibility))))

(defn plane [name & {:keys [size
                            position
                            rotation
                            width
                            height
                            billboard-mode
                            visibility
                            scale] :as opts}]
  (let [p (j/call MeshBuilder :CreatePlane name #js {:size size
                                                     :width width
                                                     :height height})]
    (m/cond-doto p
      billboard-mode (j/assoc! :billboardMode (j/get Mesh billboard-mode))
      visibility (j/assoc! :visibility visibility)
      position (j/assoc! :position position)
      rotation (j/assoc! :rotation rotation)
      scale (scaling scale))
    (add-node-to-db name p opts)))

(defn text [name & {:keys [text
                           size
                           resolution
                           depth
                           visibility
                           position]
                    :or {size 1
                         resolution 64
                         depth 0.5}
                    :as opts}]
  (let [text (j/call MeshBuilder :CreateText name
                     text
                     font/droid
                     #js {:size size
                          :resolution resolution
                          :depth depth}
                     nil
                     earcut)]
    (add-node-to-db name text (assoc opts :type :text3D))
    (cond-> text
      visibility (j/assoc! :visibility visibility)
      position (j/assoc! :position position))))

(defn get-pos [obj]
  (j/call obj :getAbsolutePosition))

(defn set-pos [obj v3]
  (j/call obj :setPosition v3))

(defn update-pos [obj v3]
  (j/assoc! obj :position v3))

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

(defn create-ground [name & {:keys [width height mat]
                             :as opts}]
  (let [ground (j/call MeshBuilder :CreateGround name #js {:width width
                                                           :height height})]
    (add-node-to-db name ground opts)
    (cond-> ground
      mat (j/assoc! :material mat))))

(defn create-ground-from-hm [name & {:keys [texture subdivisions width height max-height min-height on-ready mat]
                                     :as opts}]
  (let [ground (j/call MeshBuilder :CreateGroundFromHeightMap name texture #js {:subdivisions subdivisions
                                                                                :width width
                                                                                :height height
                                                                                :maxHeight max-height
                                                                                :minHeight min-height
                                                                                :onReady on-ready})]
    (add-node-to-db name ground opts)
    (cond-> ground
      mat (j/assoc! :material mat))))

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
                                   specular-texture
                                   emissive-texture
                                   bump-texture
                                   opacity-texture
                                   diffuse-color
                                   specular-color
                                   back-face-culling?
                                   reflection-texture
                                   coordinates-mode
                                   disable-lighting?
                                   get-alpha-from-rgb?
                                   emissive-color]
                            :as opts}]
  (cond-> (StandardMaterial. name)
    diffuse-texture (j/assoc! :diffuseTexture diffuse-texture)
    specular-texture (j/assoc! :specularTexture specular-texture)
    emissive-texture (j/assoc! :emissiveTexture emissive-texture)
    bump-texture (j/assoc! :bumpTexture bump-texture)
    opacity-texture (j/assoc! :opacityTexture opacity-texture)
    get-alpha-from-rgb? (j/assoc-in! [:opacityTexture :getAlphaFromRGB] get-alpha-from-rgb?)
    specular-color (j/assoc! :specularColor specular-color)
    (some? back-face-culling?) (j/assoc! :backFaceCulling back-face-culling?)
    reflection-texture (j/assoc! :reflectionTexture reflection-texture)
    coordinates-mode (j/assoc-in! [:reflectionTexture :coordinatesMode] (j/get Texture coordinates-mode))
    (some? disable-lighting?) (j/assoc! :disableLighting disable-lighting?)
    diffuse-color (j/assoc! :diffuseColor diffuse-color)
    emissive-color (j/assoc! :emissiveColor emissive-color)))

(defn grid-mat [name & {:keys [major-unit-frequency
                               minor-unit-visibility
                               grid-ratio
                               back-face-culling?
                               main-color
                               line-color
                               opacity]
                        :as opts}]
  (m/cond-doto (GridMaterial. name)
    major-unit-frequency (j/assoc! :majorUnitFrequency major-unit-frequency)
    minor-unit-visibility (j/assoc! :minorUnitVisibility minor-unit-visibility)
    grid-ratio (j/assoc! :gridRatio grid-ratio)
    (some? back-face-culling?) (j/assoc! :backFaceCulling back-face-culling?)
    main-color (j/assoc! :mainColor main-color)
    line-color (j/assoc! :lineColor line-color)
    opacity (j/assoc! :opacity opacity)))

(defn create-sky-box []
  (let [skybox (box "sky-box"
                    :size 1000.0
                    :skybox? true
                    :infinite-distance? false)
        mat (standard-mat "sky-box-mat"
                          :back-face-culling? false
                          :reflection-texture (CubeTexture. "" nil nil nil #js ["img/skybox/space2/px.png"
                                                                                "img/skybox/space2/py.png"
                                                                                "img/skybox/space2/pz.png"
                                                                                "img/skybox/space2/nx.png"
                                                                                "img/skybox/space2/ny.png"
                                                                                "img/skybox/space2/nz.png"])
                          :coordinates-mode :SKYBOX_MODE
                          :diffuse-color (color 0 0 0)
                          :specular-color (color 0 0 0)
                          :disable-lighting? true)]
    (j/assoc! skybox :material mat)
    skybox))

(defn texture [path & {:keys [u-scale v-scale]
                       :as opts}]
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

(defn directional-light [name & {:keys [dir pos intensity]
                                 :or {dir (v3 0 -1 0)
                                      intensity 1}
                                 :as opts}]
  (let [light (DirectionalLight. name dir)]
    (add-node-to-db name light opts)
    (j/assoc! light :position pos
              :intensity intensity)))

(defn hemispheric-light [name & {:keys [dir pos]
                                 :or {dir (v3 0 1 0)}
                                 :as opts}]
  (let [light (HemisphericLight. name dir)]
    (add-node-to-db name light opts)
    (j/assoc! light :position pos)))

(defn shadow-generator [name & {:keys [map-size light]
                                :or {map-size 1024}
                                :as opts}]
  ;; TODO there is no name so be careful on disposing
  (add-node-to-db name (ShadowGenerator. map-size light) opts))

(defn add-shadow-caster [shadow-generator mesh]
  (j/call shadow-generator :addShadowCaster mesh))

(defn create-free-camera [name & {:keys [position speed min-z]
                                  :or {position (v3 0 2 -10)
                                       speed 0.5
                                       min-z 0.1}
                                  :as opts}]
  (let [camera (FreeCamera. name position)
        init-rotation (clone (j/get camera :rotation))
        init-position (clone (j/get camera :position))]
    (add-node-to-db name camera (assoc opts :type :free
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
    (add-node-to-db name camera (assoc opts :type :arc
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
  (let [advanced-texture (j/call-in AdvancedDynamicTexture [:CreateFullscreenUI] "UI")]
    (j/assoc! db :gui-advanced-texture advanced-texture)
    advanced-texture))

(defn get-advanced-texture []
  (j/get db :gui-advanced-texture))

(defn gui-image [name url]
  (Image. name url))

(defn gui-button [name text]
  (j/call Button :CreateSimpleButton name text))

(defn add-control [container control]
  (j/call container :addControl control))

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

(defn dispose-engine []
  (j/call-in db [:engine :dispose])
  (set! db #js {}))

(defn show-debug []
  (let [p (j/call-in db [:scene :debugLayer :show] #js {:embedMode true})]
    (.then p
           (fn []
             (j/assoc-in! (js/document.getElementById "embed-host") [:style :position] "absolute")))))

(defn hide-debug []
  (j/call-in db [:scene :debugLayer :hide]))

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

(defn cubic-ease [mode]
  (doto (CubicEase.)
    (j/call :setEasingMode (j/get EasingFunction mode))))

(defn begin-direct-animation [& {:keys [target
                                        animations
                                        from
                                        to
                                        loop?
                                        speed-ratio
                                        on-animation-end
                                        delay]
                                 :or {loop? false
                                      speed-ratio 1.0}}]
  (let [p (a/promise-chan)
        on-animation-end (fn []
                           (when on-animation-end
                             (on-animation-end target))
                           (a/put! p true))
        f #(j/call-in db [:scene :beginDirectAnimation]
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

(defn active-camera []
  (j/get-in db [:scene :activeCamera]))

(defn update-active-camera []
  (j/assoc-in! db [:nodes :camera :obj] (active-camera)))

(defn detach-control [camera]
  (j/call camera :detachControl))

(defn dynamic-texture [name & {:keys [resolution
                                      width
                                      height
                                      alpha?
                                      generate-mipmaps?]
                               :or {resolution 1024
                                    generate-mipmaps? true}}]
  (let [[width height] (if resolution
                         [resolution (/ resolution 2)]
                         [width height])
        texture (DynamicTexture. name #js {:width width
                                           :height height
                                           :generateMipMaps generate-mipmaps?})]
    (m/cond-doto texture
      (some? alpha?) (j/assoc! :hasAlpha alpha?))))

(defn gui-rectangle [name & {:keys [corner-radius
                                    background
                                    height]
                             :as opts}]
  (let [rect (Rectangle. name)]
    (add-node-to-db name rect opts)
    (m/cond-doto rect
      height (j/assoc! :height height)
      corner-radius (j/assoc! :cornerRadius corner-radius)
      background (j/assoc! :background background))))

(defn gui-create-for-mesh [mesh & {:keys [width height]}]
  (j/call AdvancedDynamicTexture :CreateForMesh mesh width height))

(defn gui-text-block [name & {:keys [text
                                     alpha
                                     font-family
                                     font-size-in-pixels
                                     text-wrapping
                                     text-horizontal-alignment
                                     text-vertical-alignment
                                     padding-top
                                     padding-bottom
                                     padding-right
                                     padding-left
                                     font-size
                                     line-spacing
                                     color
                                     font-weight]
                              :as opts}]
  (let [text-block (TextBlock. name text)]
    (add-node-to-db name text-block (assoc opts :type :text))
    (m/cond-doto text-block
      font-size-in-pixels (j/assoc! :fontSizeInPixels font-size-in-pixels)
      text-wrapping (j/assoc! :textWrapping (j/get TextWrapping text-wrapping))
      text-horizontal-alignment (j/assoc! :textHorizontalAlignment (j/get Control text-horizontal-alignment))
      text-vertical-alignment (j/assoc! :textVerticalAlignment (j/get Control text-vertical-alignment))
      alpha (j/assoc! :alpha alpha)
      font-family (j/assoc! :fontFamily font-family)
      line-spacing (j/assoc! :lineSpacing line-spacing)
      padding-top (j/assoc! :paddingTop padding-top)
      padding-bottom (j/assoc! :paddingBottom padding-bottom)
      padding-right (j/assoc! :paddingRight padding-right)
      padding-left (j/assoc! :paddingLeft padding-left)
      font-size (j/assoc! :fontSize font-size)
      color (j/assoc! :color color)
      font-weight (j/assoc! :fontWeight font-weight))))

(defn scene-clear-color [color]
  (j/assoc-in! db [:scene :clearColor] color))

(defn highlight-layer [name & {:keys [blur-horizontal-size
                                      blur-vertical-size
                                      inner-glow?
                                      outer-glow?]}]
  (m/cond-doto (HighlightLayer. name)
    blur-horizontal-size (j/assoc! :blurHorizontalSize blur-horizontal-size)
    blur-vertical-size (j/assoc! :blurVerticalSize blur-vertical-size)
    inner-glow? (j/assoc! :innerGlow inner-glow?)
    outer-glow? (j/assoc! :outerGlow outer-glow?)))

(defn glow-layer [name & {:keys [main-texture-samples
                                 main-texture-fixed-size
                                 blur-kernel-size
                                 intensity]
                          :or {main-texture-samples 1
                               blur-kernel-size 32}}]
  (let [gl (GlowLayer. name (j/get db :scene) #js {:mainTextureSamples main-texture-samples
                                                   :mainTextureFixedSize main-texture-fixed-size
                                                   :blurKernelSize blur-kernel-size})]
    (m/cond-doto gl
      intensity (j/assoc! :intensity intensity))))

(defn transform-node [name & {:keys [position
                                     rotation
                                     scale]
                              :as opts}]
  (let [tn (TransformNode. name)]
    (add-node-to-db name tn opts)
    (m/cond-doto tn
      position (j/assoc! :position position)
      rotation (j/assoc! :rotation rotation)
      scaling (scaling scale))))
