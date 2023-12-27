(ns immersa.scene.api.material
  (:require
    ["@babylonjs/core/Materials/Node/nodeMaterial" :refer [NodeMaterial]]
    ["@babylonjs/core/Materials/Textures/texture" :refer [Texture]]
    ["@babylonjs/core/Materials/effect" :refer [Effect]]
    ["@babylonjs/core/Materials/shaderMaterial" :refer [ShaderMaterial]]
    ["@babylonjs/core/Materials/standardMaterial" :refer [StandardMaterial]]
    ["@babylonjs/materials/grid/gridMaterial" :refer [GridMaterial]]
    [applied-science.js-interop :as j]
    [immersa.scene.api.core :as api.core])
  (:require-macros
    [immersa.scene.macros :as m]))

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
  (let [sm (StandardMaterial. name)]
    (api.core/add-node-to-db name sm opts)
    (cond-> sm
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

(defn parse-from-json [json-str]
  (j/call NodeMaterial :Parse (js/JSON.parse json-str)))

(defn parse-from-snippet [id on-loaded]
  (j/call (j/call NodeMaterial :ParseFromSnippetAsync id) :then on-loaded))

(defn get-block-by-name [mat name]
  (j/call mat :getBlockByName name))
