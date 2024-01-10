(ns immersa.scene.api.material
  (:require
    ["@babylonjs/core/Helpers/environmentHelper" :refer [EnvironmentHelper]]
    ["@babylonjs/core/Materials/Background/backgroundMaterial" :refer [BackgroundMaterial]]
    ["@babylonjs/core/Materials/Node/nodeMaterial" :refer [NodeMaterial]]
    ["@babylonjs/core/Materials/Textures/texture" :refer [Texture]]
    ["@babylonjs/core/Materials/effect" :refer [Effect]]
    ["@babylonjs/core/Materials/shaderMaterial" :refer [ShaderMaterial]]
    ["@babylonjs/core/Materials/standardMaterial" :refer [StandardMaterial]]
    ["@babylonjs/materials/grid/gridMaterial" :refer [GridMaterial]]
    [applied-science.js-interop :as j]
    [immersa.scene.api.core :as api.core])
  (:require-macros
    [immersa.scene.macros :as m]
    [shadow.resource :as rc]))

(defn parse-from-json [json-str]
  (j/call NodeMaterial :Parse (js/JSON.parse json-str)))

(defn parse-from-snippet [id on-loaded]
  (j/call (j/call NodeMaterial :ParseFromSnippetAsync id) :then on-loaded))

;; TODO move to addTask, currently it's bundled with the app.js!!!
(def nme)

(defn init-nme-materials []
  (set! nme {:purple-glass (parse-from-json (rc/inline "shader/nme/purpleGlass.json"))}))

(defn get-nme-material [name]
  (api.core/clone (get nme name)))

(defn standard-mat [name & {:keys [diffuse-texture
                                   has-alpha?
                                   specular-texture
                                   emissive-texture
                                   emissive-color
                                   bump-texture
                                   opacity-texture
                                   diffuse-color
                                   specular-color
                                   back-face-culling?
                                   reflection-texture
                                   coordinates-mode
                                   disable-lighting?
                                   get-alpha-from-rgb?]
                            :as opts}]
  (let [sm (StandardMaterial. name)]
    (api.core/add-node-to-db name sm opts)
    (cond-> sm
      diffuse-texture (j/assoc! :diffuseTexture diffuse-texture)
      (some? has-alpha?) (j/assoc! :hasAlpha has-alpha?)
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
      emissive-color (j/assoc! :emissiveColor emissive-color))))

(defn grid-mat [name & {:keys [major-unit-frequency
                               minor-unit-visibility
                               grid-ratio
                               back-face-culling?
                               main-color
                               line-color
                               opacity]
                        :as opts}]
  (let [gm (GridMaterial. name)]
    (api.core/add-node-to-db name gm opts)
    (m/cond-doto (GridMaterial. name)
      major-unit-frequency (j/assoc! :majorUnitFrequency major-unit-frequency)
      minor-unit-visibility (j/assoc! :minorUnitVisibility minor-unit-visibility)
      grid-ratio (j/assoc! :gridRatio grid-ratio)
      (some? back-face-culling?) (j/assoc! :backFaceCulling back-face-culling?)
      main-color (j/assoc! :mainColor main-color)
      line-color (j/assoc! :lineColor line-color)
      opacity (j/assoc! :opacity opacity))))

(defn shader-mat [name & {:keys [fragment
                                 vertex
                                 attrs
                                 uniforms]
                          :as opts}]
  (j/assoc-in! Effect [:ShadersStore (str name "VertexShader")] vertex)
  (j/assoc-in! Effect [:ShadersStore (str name "FragmentShader")] fragment)
  (let [sm (ShaderMaterial. name nil #js{:vertex name
                                         :fragment name}
                            (clj->js {:attributes attrs
                                      :uniforms uniforms}))]
    (api.core/add-node-to-db name sm opts)))

(defn background-mat [name & {:keys [reflection-texture
                                     primary-color
                                     shadow-level]
                              :or {shadow-level 0.4}
                              :as opts}]
  (let [bm (BackgroundMaterial. name)]
    (m/assoc! bm
              :backFaceCulling false
              :reflectionTexture reflection-texture
              :reflectionTexture.coordinatesMode (j/get Texture :SKYBOX_MODE)
              :reflectionTexture.gammaSpace false
              :shadowLevel shadow-level
              :opacityFresnel true
              :enableNoise true
              :useRGBColor false
              :primaryColor primary-color)
    (api.core/add-node-to-db name bm opts)))

(defn create-environment-helper []
  (let [engine (api.core/get-engine)
        srgb-conversions (j/get engine :useExactSrgbConversions)
        color-fn (fn [color]
                   (j/call (j/call color :toLinearSpace srgb-conversions) :scale 3))
        options #js {:createGround true
                     :groundSize 15
                     :groundTexture "img/texture/ground/backgroundGround.png"
                     :groundColor (color-fn (api.core/color 0.2 0.2 0.3))
                     :groundOpacity 0.9
                     :enableGroundShadow true
                     :groundShadowLevel 0.5
                     :enableGroundMirror false
                     :groundMirrorSizeRatio 0.3
                     :groundMirrorBlurKernel 64
                     :groundMirrorAmount 1
                     :groundMirrorFresnelWeight 1
                     :groundMirrorFallOffDistance 0
                     :groundMirrorTextureType 0
                     :groundYBias 0.00001
                     :createSkybox true
                     :skyboxSize 1000
                     :skyboxTexture "img/texture/skybox/backgroundSkybox.dds"
                     :skyboxColor (color-fn (api.core/color-rgb 91 91 111))
                     :backgroundYRotation 0
                     :sizeAuto true
                     :rootPosition (api.core/v3)
                     :setupImageProcessing true
                     :environmentTexture "img/texture/environment/environmentSpecular.env"
                     :cameraExposure 0.8
                     :cameraContrast 1.2
                     :toneMappingEnabled true}
        eh (EnvironmentHelper. options (api.core/get-scene))]
    (j/assoc! api.core/db :environment-helper eh)
    eh))

(defn get-block-by-name [mat name]
  (j/call mat :getBlockByName name))
