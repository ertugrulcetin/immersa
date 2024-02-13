(ns immersa.scene.api.component
  (:require
    [applied-science.js-interop :as j]
    [immersa.scene.api.constant :as api.const]
    [immersa.scene.api.core :as api.core :refer [v2 v3 v4]]
    [immersa.scene.api.gui :as api.gui]
    [immersa.scene.api.material :as api.material]
    [immersa.scene.api.mesh :as api.mesh])
  (:require-macros
    [immersa.scene.macros :as m]
    [shadow.resource :as rc]))

(defn create-sky-box [& {:keys [skybox1
                                skybox2
                                noise
                                speed-factor]
                         :or {speed-factor 1
                              skybox1 "img/skybox/black/black"
                              skybox2 "img/skybox/black_copy/black"
                              ;; skybox2 "img/skybox/space/space"
                              noise "img/texture/noise/ac.jpg"}}]
  (let [skybox (api.mesh/box "sky-box"
                             :size 1000.0
                             :skybox? true
                             :infinite-distance? false
                             :pickable? false
                             :alpha-index 0)
        mat (api.material/shader-mat
              "skybox-shader"
              :vertex (rc/inline "shader/skybox/vertex.glsl")
              :fragment (rc/inline "shader/skybox/fragment.glsl")
              :attrs ["position"]
              :uniforms ["worldViewProjection" "dissolve" "skybox1" "skybox2" "noiseTexture" "transparency"])]
    (j/call mat :setTexture "skybox1" (api.core/cube-texture :root-url skybox1))
    (j/call mat :setTexture "skybox2" (api.core/cube-texture :root-url skybox2))
    (j/call mat :setTexture "noiseTexture" (api.core/texture noise))
    (j/call mat :setFloat "dissolve" 0)
    (j/call mat :setFloat "transparency" 0)
    (j/assoc! mat
              :backFaceCulling false
              :skybox-path skybox1
              :default-skybox-path skybox1)
    (j/assoc! skybox :material mat)
    #_(j/assoc! skybox :material (j/get-in api.core/db [:environment-helper :skyboxMaterial]))
    skybox))

(comment
  (create-sky-box)
  (api.core/dispose "skybox-shader")
  (js/setTimeout #(api.core/dispose "sky-box") 2000)
  (j/call (api.core/get-object-by-name "skybox-shader") :setFloat "transparency" 0.2)
  (j/call (api.core/get-object-by-name "skybox-shader") :getFloat "transparency")
  (j/assoc! (api.core/get-object-by-name "skybox-shader") :alpha 0)

  (j/get (api.core/get-object-by-name "skybox-shader") :skybox-path)

  (j/get (api.core/get-object-by-name "skybox-shader") :alpha)
  (j/assoc! (api.core/get-object-by-name "skybox-shader") :alpha 0)
  (api.core/dispose (api.core/get-object-by-name "skybox-shader"))

  (j/assoc! (j/get-in api.core/db [:environment-helper :skybox]) :visibility 1)
  )

(defn create-sky-sphere []
  (let [sky-sphere (api.mesh/sphere "sky-sphere"
                                    :diameter 1000.0
                                    :side-orientation api.const/mesh-double-side
                                    :skybox? true
                                    :infinite-distance? false)
        mat (api.material/shader-mat "sky-sphere-mat"
                                     :vertex (rc/inline "shader/gradient/vertex.glsl")
                                     :fragment (rc/inline "shader/gradient/fragment.glsl")
                                     :attrs ["position" "uv" "v_uv"]
                                     :uniforms ["worldViewProjection"
                                                "u_mixColor"
                                                "u_mixColor1"
                                                "u_mixScale"
                                                "u_Time"
                                                "u_mixSpeed"
                                                "u_mixColor2"
                                                "u_mixScale1"
                                                "u_Time1"
                                                "u_mixSpeed1"
                                                "u_mixOffset"
                                                "u_visibility"])
        elapsed-time (atom 0)]
    (doto mat
      (j/call :setVector3 "u_mixColor" (v3 0.0 0.0 0.0))
      (j/call :setVector3 "u_mixColor1" (v3 0.92 0.18 1.0))
      (j/call :setVector3 "u_mixColor2" (v3 0.04 0.04 0.99))
      (j/call :setVector2 "u_mixScale" (v2 2.25 2.75))
      (j/call :setVector2 "u_mixScale1" (v2 2.79 2.55))
      (j/call :setFloat "u_mixSpeed" 0.2)
      (j/call :setFloat "u_mixSpeed1" 0.2)
      (j/call :setFloat "u_Time" 0.0)
      (j/call :setFloat "u_Time1" 0.0)
      (j/call :setFloat "u_mixOffset" 1.0)
      (j/call :setFloat "u_visibility" 0.0))
    (api.core/register-before-render-fn "sky-sphere-before-render"
                                        (fn []
                                          (let [delta (api.core/get-delta-time)
                                                elapsed-time (swap! elapsed-time + delta)]
                                            (j/call mat :setFloat "u_Time" elapsed-time)
                                            (j/call mat :setFloat "u_Time1" elapsed-time))))
    (j/assoc! mat :backFaceCulling false)
    (j/assoc! sky-sphere
              :material mat
              :visibility 0)))

(defn billboard [name & {:keys [text
                                position
                                rotation
                                width
                                height
                                visibility
                                color
                                scale
                                resolution-scale
                                font-size
                                font-weight
                                font-family
                                rect-width
                                rect-height
                                rect-corner-radius
                                rect-background
                                padding-left
                                horizontal-alignment
                                thickness]
                         :or {width 1.2
                              height 1
                              scale (v3 1)
                              resolution-scale 5
                              font-size 100
                              font-family "Bellefair,serif"
                              color "white"
                              rect-height 1
                              rect-width 3
                              rect-corner-radius 100
                              thickness 0
                              horizontal-alignment api.const/gui-horizontal-align-center
                              ;; rect-background "rgba(128, 128, 128, 0.4)"
                              }}]
  (let [rect-height (/ 1 resolution-scale)
        rect-width (/ 3 resolution-scale)
        plane (api.mesh/plane name
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
                                 :font-size (* font-size resolution-scale)
                                 :font-family font-family
                                 :text-wrapping api.const/gui-text-wrapping-word-wrap
                                 :text-horizontal-alignment horizontal-alignment
                                 :padding-left (some-> padding-left (* resolution-scale))
                                 :color color
                                 :font-weight font-weight)
        rect (api.gui/rectangle (str name "-rect")
                                :corner-radius (* resolution-scale rect-corner-radius)
                                :width rect-width
                                :height rect-height
                                :thickness thickness
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
                                       :diffuse-texture (api.core/texture "img/texture/earth/diffuse2-min.png")
                                       :emissive-texture (api.core/texture "img/texture/earth/emmisive.jpeg")
                                       :specular-texture (api.core/texture "img/texture/earth/specular.jpeg")
                                       :bump-texture (api.core/texture "img/texture/earth/bump.jpeg"))
        mat-clouds (api.material/standard-mat (str name "-clouds")
                                              :opacity-texture (api.core/texture "img/texture/earth/clouds2-min.jpg")
                                              :get-alpha-from-rgb? true)
        tn (api.core/mesh name :position position)
        sp (api.mesh/sphere (str name "-earth-sphere")
                            :mat mat
                            :visibility visibility
                            :scale (v3 1.2)
                            :rotation (v3 0 0 js/Math.PI))
        clouds (api.mesh/sphere (str name "-cloud-sphere")
                                :mat mat-clouds
                                :visibility visibility
                                :scale (v3 1.21)
                                :rotation (v3 0 0 js/Math.PI))
        ;; TODO it's suggested to use only one highlight layer for a scene
        hl (api.core/highlight-layer "hl"
                                     :blur-vertical-size 3
                                     :blur-horizontal-size 3)]
    (api.core/add-children tn sp clouds)
    (api.core/add-prop-to-db name :type :earth)
    (api.core/add-prop-to-db name :hl hl)
    (j/call hl :addMesh clouds (api.core/color 0.3 0.74 0.94))
    tn))

(defn wave [name & {:keys [position
                           rotation
                           width
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
                    :or {position (v3 0 -5 35)
                         rotation (v3 (/ js/Math.PI 70) 0 0)
                         width 50
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
              name
              :type :wave
              :point-count (* width resolution)
              :on-add-point (fn [particle i]
                              (let [x (- (/ (* (mod i resolution) width) resolution) (/ width 2))
                                    z (- (/ (* (Math/floor (/ i resolution)) height) resolution) (/ height 2))]
                                (j/assoc! particle :position (v3 x 0 z))))
              :on-build-done (fn [mesh]
                               (m/assoc! mesh
                                         :material mat
                                         :material.pointsCloud true
                                         :position position
                                         :rotation rotation
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
    pcs))

(defn image [name & {:keys [path
                            position
                            rotation
                            visibility
                            scale
                            radius
                            billboard-mode
                            face-to-screen?
                            double-side?
                            transparent?]
                     :or {transparent? false
                          double-side? false}}]
  (let [texture (api.core/texture path)
        {:keys [width height]} (j/lookup (j/call texture :getSize))
        width (/ width height)
        height 1
        mat (api.material/standard-mat
              (str name "-image-mat")
              (cond-> {:diffuse-texture texture
                       :emissive-color (api.const/color-white)}
                transparent? (assoc :opacity-texture texture
                                    :has-alpha? transparent?)))
        opts {:width width
              :height height
              :face-to-screen? face-to-screen?
              :radius radius
              :position position
              :rotation rotation
              :visibility visibility
              :scale scale
              :billboard-mode billboard-mode
              :double-side? double-side?
              :material mat
              :type :image}
        mesh (if (and radius (> radius 0))
               (api.mesh/plane-rounded name opts)
               (api.mesh/plane name opts))]
    (m/cond-doto mesh
      face-to-screen? (j/assoc! :billboardMode api.const/mesh-billboard-mode-all))
    (j/assoc! mesh :initial-rotation (api.core/clone (j/get mesh :rotation)))))

(comment
  (js/console.log (api.core/get-object-by-name "98e4ee76-bb27-4904-9d30-360a40d8abc1"))
  (j/assoc-in! (api.core/get-object-by-name "98e4ee76-bb27-4904-9d30-360a40d8abc1") [:position :z] 30)
  (j/assoc-in! (api.core/get-object-by-name "98e4ee76-bb27-4904-9d30-360a40d8abc1") [:position :y] -12)

  (api.core/get-object-by-name "33e4ee76-bb27-4904-9d30-360a40d8abc1")

  (j/assoc! (api.core/get-object-by-name "33e4ee76-bb27-4904-9d30-360a40d8abc1") :renderingGroupId 1)

  (api.core/look-at (api.core/get-object-by-name "98e4ee76-bb27-4904-9d30-360a40d8abc1")
                    (j/get (immersa.scene.api.camera/active-camera) :position))

  (j/call (api.core/get-object-by-name "98e4ee76-bb27-4904-9d30-360a40d8abc1")
          :setParent (immersa.scene.api.camera/active-camera))

  (j/call (api.core/get-object-by-name "98e4ee76-bb27-4904-9d30-360a40d8abc1")
          :setParent nil)

  (api.core/attach-to-mesh (api.core/get-object-by-name "98e4ee76-bb27-4904-9d30-360a40d8abc1"))

  (image "img"
         :transparent? true
         :path "https://firebasestorage.googleapis.com/v0/b/immersa-6d29f.appspot.com/o/images%2Fschaltbau%2Flogo.png?alt=media&token=2afccb59-5489-4553-9a98-0425f0bac1db")
  )
