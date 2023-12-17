(ns immersa.scene.api.light
  (:require
    ["@babylonjs/core/Lights/Shadows/shadowGenerator" :refer [ShadowGenerator]]
    ["@babylonjs/core/Lights/directionalLight" :refer [DirectionalLight]]
    ["@babylonjs/core/Lights/hemisphericLight" :refer [HemisphericLight]]
    [applied-science.js-interop :as j]
    [immersa.scene.api.core :as api.core :refer [v3]]))

(defn directional-light [name & {:keys [dir pos intensity]
                                 :or {dir (v3 0 -1 0)
                                      intensity 1}
                                 :as opts}]
  (let [light (DirectionalLight. name dir)]
    (api.core/add-node-to-db name light opts)
    (j/assoc! light :position pos
              :intensity intensity)))

(defn hemispheric-light [name & {:keys [dir pos]
                                 :or {dir (v3 0 1 0)}
                                 :as opts}]
  (let [light (HemisphericLight. name dir)]
    (api.core/add-node-to-db name light opts)
    (j/assoc! light :position pos)))

(defn shadow-generator [name & {:keys [map-size light]
                                :or {map-size 1024}
                                :as opts}]
  ;; TODO there is no name so be careful on disposing
  (api.core/add-node-to-db name (ShadowGenerator. map-size light) opts))

(defn add-shadow-caster [shadow-generator mesh]
  (j/call shadow-generator :addShadowCaster mesh))
