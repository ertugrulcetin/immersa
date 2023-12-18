(ns immersa.scene.api.component
  (:require
    ["@babylonjs/core/Materials/Textures/cubeTexture" :refer [CubeTexture]]
    [applied-science.js-interop :as j]
    [immersa.scene.api.constant :as api.const]
    [immersa.scene.api.core :as api.core :refer [v2 v3 v4]]
    [immersa.scene.api.gui :as api.gui]
    [immersa.scene.api.material :as api.material]
    [immersa.scene.api.mesh :as api.mesh])
  (:require-macros
    [immersa.scene.macros :as m]
    [shadow.resource :as rc]))

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
                              :billboard-mode api.const/mesh-billboard-mode-all
                              :visibility visibility
                              :scale scale
                              :type :billboard)
        gui (api.gui/create-for-mesh plane :width (* resolution-scale 1024) :height (* resolution-scale 1024))
        text (api.gui/text-block (str name "-text-block")
                                 :text text
                                 :font-size-in-pixels (* 60 resolution-scale)
                                 :text-wrapping api.const/gui-text-wrapping-word-wrap
                                 :text-horizontal-alignment api.const/gui-horizontal-align-left
                                 :padding-left (* 50 resolution-scale)
                                 :color color
                                 :font-weight font-weight)
        rect (api.gui/rectangle (str name "-rect")
                                :corner-radius rect-corner-radius
                                :height rect-height
                                :background rect-background)]
    (api.core/add-prop-to-db name :children [plane gui text rect])
    (api.gui/add-control gui rect)
    (api.gui/add-control rect text)
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

(defn earth [name & {:keys [position visibility] :as opts}]
  (let [mat (api.material/standard-mat (str name "-mat")
                                       :diffuse-texture (api.core/texture "img/texture/earth/diffuse2.png")
                                       :emissive-texture (api.core/texture "img/texture/earth/emmisive.jpeg")
                                       :specular-texture (api.core/texture "img/texture/earth/specular.jpeg")
                                       :bump-texture (api.core/texture "img/texture/earth/bump.jpeg"))
        mat-clouds (api.material/standard-mat (str name "-clouds")
                                              :opacity-texture (api.core/texture "img/texture/earth/clouds2.jpg")
                                              :get-alpha-from-rgb? true)
        tn (api.core/mesh name :position position)
        sp (api.mesh/sphere (str name "-earth-sphere")
                            :mat mat
                            :visibility visibility
                            :scale 1.2
                            :rotation (v3 0 0 js/Math.PI))
        clouds (api.mesh/sphere (str name "-cloud-sphere")
                                :mat mat-clouds
                                :visibility visibility
                                :scale 1.21
                                :rotation (v3 0 0 js/Math.PI))
        ;; TODO it's suggested to use only one highlight layer for a scene
        hl (api.core/highlight-layer "hl"
                                     :blur-vertical-size 3
                                     :blur-horizontal-size 3)]
    (api.core/add-children tn sp clouds)
    (api.core/add-prop-to-db name :type :earth)
    (j/call hl :addMesh clouds (api.core/color 0.3 0.74 0.94 0.82))
    tn))

(defn wave [name & {:keys [width
                           height
                           resolution
                           point-size
                           time-scale
                           noise-amp-1
                           noise-freq-1
                           spd-modifier-1
                           noise-amp-2
                           noise-freq-2
                           spd-modifier-2]
                    :or {width 50
                         height 50
                         resolution 100
                         point-size 3.0
                         time-scale 1.0
                         noise-amp-1 0.2
                         noise-freq-1 3.0
                         spd-modifier-1 1.0
                         noise-amp-2 0.3
                         noise-freq-2 2.0
                         spd-modifier-2 0.8}
                    :as opts}]
  (let [mat (api.material/shader-mat
              (str name "-wave-shader")
              :fragment (rc/inline "shader/wave/fragment.glsl")
              :vertex (rc/inline "shader/wave/vertex.glsl")
              :attrs ["position" "normal" "uv"]
              :uniforms ["world" "worldView" "worldViewProjection" "view" "projection"
                         "u_time" "u_pointsize" "u_noise_amp_1" "u_noise_freq_1"
                         "u_spd_modifier_1" "u_noise_amp_2" "u_noise_freq_2"
                         "u_spd_modifier_2"])
        pcs (api.core/point-cloud-system
              (str "-wave-pcs")
              :point-count (* width resolution)
              :on-add-point (fn [particle i]
                              (let [x (- (/ (* (mod i resolution) width) resolution) (/ width 2))
                                    z (- (/ (* (Math/floor (/ i resolution)) height) resolution) (/ height 2))]
                                (j/assoc! particle :position (v3 x 0 z))))
              :on-build-done (fn [mesh]
                               (m/assoc! mesh
                                         :material mat
                                         :material.pointsCloud true
                                         :position (v3 0 -5 35)
                                         :rotation (v3 (/ js/Math.PI 70) 0 0)
                                         :visibility 0.5)))
        start-time (js/Date.now)
        engine (api.core/get-engine)
        temp-v2 (v2)
        _ (api.core/register-before-render-fn
            (str name "-wave-before-render")
            (fn []
              (j/call mat :setFloat "u_pointsize" point-size)
              (j/call mat :setFloat "u_noise_amp_1" noise-amp-1)
              (j/call mat :setFloat "u_noise_freq_1" noise-freq-1)
              (j/call mat :setFloat "u_spd_modifier_1" spd-modifier-1)
              (j/call mat :setFloat "u_noise_amp_2" noise-amp-2)
              (j/call mat :setFloat "u_noise_freq_2" noise-freq-2)
              (j/call mat :setFloat "u_spd_modifier_2" spd-modifier-2)
              (j/call mat :setFloat "u_time" (* (/ (- (js/Date.now) start-time) 1000) time-scale))
              (j/call mat :setVector2 "u_resolution" (api.core/set-v2 temp-v2
                                                                      (j/call engine :getRenderWidth)
                                                                      (j/call engine :getRenderHeight)))))]
    (api.core/add-node-to-db name pcs (assoc opts :type :pcs))))
