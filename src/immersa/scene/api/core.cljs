(ns immersa.scene.api.core
  (:refer-clojure :exclude [clone])
  (:require
    ["@babylonjs/core/Actions/actionManager" :refer [ActionManager]]
    ["@babylonjs/core/Actions/directActions" :refer [ExecuteCodeAction]]
    ["@babylonjs/core/Debug/debugLayer"]
    ["@babylonjs/core/Engines/engine" :refer [Engine]]
    ["@babylonjs/core/Layers/glowLayer" :refer [GlowLayer]]
    ["@babylonjs/core/Layers/highlightLayer" :refer [HighlightLayer]]
    ["@babylonjs/core/Lights/Shadows/shadowGenerator" :refer [ShadowGenerator]]
    ["@babylonjs/core/Lights/directionalLight" :refer [DirectionalLight]]
    ["@babylonjs/core/Lights/hemisphericLight" :refer [HemisphericLight]]
    ["@babylonjs/core/Loading/sceneLoader" :refer [SceneLoader]]
    ["@babylonjs/core/Materials/Textures/dynamicTexture" :refer [DynamicTexture]]
    ["@babylonjs/core/Materials/Textures/texture" :refer [Texture]]
    ["@babylonjs/core/Maths/math" :refer [Vector2 Vector3 Vector4]]
    ["@babylonjs/core/Maths/math.color" :refer [Color3]]
    ["@babylonjs/core/Meshes/transformNode" :refer [TransformNode]]
    ["@babylonjs/core/Misc/tools" :refer [Tools]]
    ["@babylonjs/core/scene" :refer [Scene]]
    ["@babylonjs/gui/2D" :refer [AdvancedDynamicTexture Control TextWrapping]]
    ["@babylonjs/gui/2D/controls" :refer [Button Image Rectangle TextBlock]]
    ["@babylonjs/inspector"]
    [applied-science.js-interop :as j]
    [immersa.scene.font :as font])
  (:require-macros
    [immersa.scene.macros :as m]))

(defonce db #js {})

(def gui-horizontal-align-left :HORIZONTAL_ALIGNMENT_LEFT)
(def gui-horizontal-align-center :HORIZONTAL_ALIGNMENT_CENTER)
(def gui-vertical-align-center :VERTICAL_ALIGNMENT_CENTER)

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

(defn- check-name-exists [name]
  (when-let [obj (j/get-in db [:nodes name :obj])]
    (dispose obj)))

(defn add-node-to-db [name obj opts]
  (check-name-exists name)
  (j/assoc-in! db [:nodes name] (clj->js (assoc opts :obj obj :name name)))
  obj)

(defn add-prop-to-db [name prop val]
  (j/assoc-in! db [:nodes name prop] val))

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

(defn texture [path & {:keys [u-scale v-scale]
                       :as opts}]
  (let [tex (Texture. path)]
    (m/cond-doto tex
      u-scale (j/assoc! :uScale u-scale)
      v-scale (j/assoc! :vScale v-scale))))

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
    (.then p #(j/assoc-in! (js/document.getElementById "embed-host") [:style :position] "absolute"))))

(defn hide-debug []
  (j/call-in db [:scene :debugLayer :hide]))

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

(defn clear-scene-color [color]
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
      scale (scaling scale))))

(defn add-children [parent & children]
  (add-prop-to-db (j/get parent :name) :children children)
  (doseq [c children]
    (j/assoc! c :parent parent)))
