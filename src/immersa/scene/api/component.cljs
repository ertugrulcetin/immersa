(ns immersa.scene.api.component
  (:require
    ["@babylonjs/core/Materials/Textures/cubeTexture" :refer [CubeTexture]]
    [applied-science.js-interop :as j]
    [immersa.scene.api.constant :as api.constant]
    [immersa.scene.api.core :as api.core :refer [v3 v4]]
    [immersa.scene.api.material :as api.material]
    [immersa.scene.api.mesh :as api.mesh]))

(defn create-sky-box []
  (let [skybox (api.mesh/box "sky-box"
                             :size 1000.0
                             :skybox? true
                             :infinite-distance? false)
        mat (api.material/standard-mat "sky-box-mat"
                                       :back-face-culling? false
                                       :reflection-texture (CubeTexture. "" nil nil nil #js ["img/skybox/space2/px.png"
                                                                                             "img/skybox/space2/py.png"
                                                                                             "img/skybox/space2/pz.png"
                                                                                             "img/skybox/space2/nx.png"
                                                                                             "img/skybox/space2/ny.png"
                                                                                             "img/skybox/space2/nz.png"])
                                       :coordinates-mode :SKYBOX_MODE
                                       :diffuse-color (api.core/color 0 0 0)
                                       :specular-color (api.core/color 0 0 0)
                                       :disable-lighting? true)]
    (j/assoc! skybox :material mat)
    skybox))

(defn billboard [name & {:keys [text
                                position
                                rotation
                                width
                                height
                                visibility
                                color
                                scale
                                resolution-scale
                                font-weight
                                rect-height
                                rect-corner-radius
                                rect-background]
                         :or {width 1.2
                              height 1
                              scale 1
                              resolution-scale 5
                              color "white"
                              rect-height "2500px"
                              rect-corner-radius 500
                              rect-background "rgba(128, 128, 128, 0.4)"}}]
  (let [plane (api.mesh/plane name
                              :width width
                              :height height
                              :position position
                              :rotation rotation
                              :billboard-mode api.constant/mesh-billboard-mode-all
                              :visibility visibility
                              :scale scale
                              :type :billboard)
        gui (api.core/gui-create-for-mesh plane :width (* resolution-scale 1024) :height (* resolution-scale 1024))
        text (api.core/gui-text-block (str name "-text-block")
                                      :text text
                                      :font-size-in-pixels (* 60 resolution-scale)
                                      :text-wrapping api.constant/gui-text-wrapping-word-wrap
                                      :text-horizontal-alignment api.constant/gui-horizontal-align-left
                                      :padding-left (* 50 resolution-scale)
                                      :color color
                                      :font-weight font-weight)
        rect (api.core/gui-rectangle (str name "-rect")
                                     :corner-radius rect-corner-radius
                                     :height rect-height
                                     :background rect-background)]
    (api.core/add-prop-to-db name :children [plane gui text rect])
    (api.core/add-control gui rect)
    (api.core/add-control rect text)
    plane))

;; TODO move to mesh, it is a tiled box
(defn create-box-with-numbers [name params]
  (let [columns 6
        rows 1
        face-uv (js/Array. columns)
        _ (dotimes [i columns]
            (aset face-uv i (v4 (/ i columns) 0 (/ (+ i 1) columns) (/ 1 rows))))
        texture (api.core/texture "img/texture/numbers.jpg")
        mat (api.material/standard-mat "mat" :diffuse-texture texture)]
    (api.mesh/box name (assoc params
                              :face-uv face-uv
                              :wrap? true
                              :mat mat))))

(defn earth [& {:keys [name position]}]
  (let [mat (api.material/standard-mat (str name "-mat")
                                       :diffuse-texture (api.core/texture "img/texture/earth/diffuse2.png")
                                       :emissive-texture (api.core/texture "img/texture/earth/emmisive.jpeg")
                                       :specular-texture (api.core/texture "img/texture/earth/specular.jpeg")
                                       :bump-texture (api.core/texture "img/texture/earth/bump.jpeg"))
        mat-clouds (api.material/standard-mat (str name "-clouds")
                                              :opacity-texture (api.core/texture "img/texture/earth/clouds2.jpg")
                                              :get-alpha-from-rgb? true)
        tn (api.core/transform-node (str name "-earth-node") :position position)
        sp (api.mesh/sphere (str name "-earth-sphere")
                            :mat mat
                            :scale 1.2
                            :rotation (v3 0 0 js/Math.PI))
        clouds (api.mesh/sphere (str name "-cloud-sphere")
                                :mat mat-clouds
                                :scale 1.21
                                :rotation (v3 0 0 js/Math.PI))
        ;; TODO it's suggested to use only one highlight layer for a scene
        hl (api.core/highlight-layer "hl"
                                     :blur-vertical-size 3
                                     :blur-horizontal-size 3)]
    (api.core/add-children tn sp clouds)
    (j/call hl :addMesh clouds (api.core/color 0.3 0.74 0.94 0.82))
    tn))
