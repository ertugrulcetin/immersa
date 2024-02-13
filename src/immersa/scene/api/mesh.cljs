(ns immersa.scene.api.mesh
  (:require
    ["@babylonjs/core/Meshes/Builders/greasedLineBuilder" :refer [CreateGreasedLine]]
    ["@babylonjs/core/Meshes/mesh" :refer [Mesh]]
    ["@babylonjs/core/Meshes/meshBuilder" :refer [MeshBuilder]]
    ["@babylonjs/core/Misc/greasedLineTools" :refer [GreasedLineTools]]
    ["earcut" :as earcut]
    [applied-science.js-interop :as j]
    [immersa.scene.api.constant :as api.const]
    [immersa.scene.api.core :as api.core :refer [v3]]
    [immersa.scene.api.material :as api.material]
    [immersa.scene.font :as font])
  (:require-macros
    [immersa.scene.macros :as m]))

(defn box [name & {:keys [size
                          width
                          height
                          depth
                          face-uv
                          wrap?
                          visibility
                          position
                          pickable?
                          skybox?
                          infinite-distance?
                          alpha-index
                          mat]
                   :as opts}]
  (let [b (j/call MeshBuilder :CreateBox name #js {:size size
                                                   :faceUV face-uv
                                                   :wrap wrap?
                                                   :width width
                                                   :height height
                                                   :depth depth})]
    (api.core/add-node-to-db name b (assoc opts :type (if skybox?
                                                        :skybox
                                                        :box)))
    (cond-> b
      alpha-index (j/assoc! :alphaIndex alpha-index)
      mat (j/assoc! :material mat)
      position (j/assoc! :position position)
      visibility (j/assoc! :visibility visibility)
      (some? pickable?) (j/assoc! :isPickable pickable?)
      (some? infinite-distance?) (j/assoc! :infiniteDistance infinite-distance?))))

(defn sphere [name & {:keys [diameter
                             segments
                             side-orientation
                             updatable?
                             arc
                             slice
                             visibility
                             position
                             rotation
                             scale
                             skybox?
                             mat
                             infinite-distance?
                             visible?
                             type]
                      :or {segments 32
                           diameter 1
                           arc 1
                           slice 1
                           updatable? false
                           side-orientation api.const/mesh-default-side}
                      :as opts}]
  (let [s (j/call MeshBuilder :CreateSphere name #js {:diameter diameter
                                                      :segments segments
                                                      :arc arc
                                                      :slice slice
                                                      :updatable updatable?
                                                      :sideOrientation side-orientation})]
    (api.core/add-node-to-db name s (assoc opts :type (or type (if skybox?
                                                                 :skybox
                                                                 :sphere))))
    (m/cond-doto s
      mat (j/assoc! :material mat)
      position (j/assoc! :position position)
      rotation (j/assoc! :rotation rotation)
      scale (j/assoc! :scaling scale)
      visibility (j/assoc! :visibility visibility)
      (some? visible?) (j/assoc! :isVisible visible?)
      (some? infinite-distance?) (j/assoc! :infiniteDistance infinite-distance?))))

(defn capsule [name & {:keys [height radius visibility]
                       :as opts}]
  (let [c (j/call MeshBuilder :CreateCapsule name #js {:height height
                                                       :radius radius})]
    (api.core/add-node-to-db name c opts)
    (cond-> c
      visibility (j/assoc! :visibility visibility))))

(defn plane [name & {:keys [position
                            rotation
                            width
                            height
                            billboard-mode
                            visibility
                            scale
                            material
                            double-side?
                            type]
                     :or {type :plane
                          double-side? false}
                     :as opts}]
  (let [p (j/call MeshBuilder :CreatePlane name #js {:width width
                                                     :height height
                                                     :sideOrientation (if double-side?
                                                                        api.const/mesh-double-side
                                                                        api.const/mesh-default-side)})]
    (m/cond-doto p
      billboard-mode (j/assoc! :billboardMode billboard-mode)
      visibility (j/assoc! :visibility visibility)
      position (j/assoc! :position position)
      rotation (j/assoc! :rotation rotation)
      scale (j/assoc! :scaling scale)
      material (j/assoc! :material material))
    (api.core/add-node-to-db name p (assoc opts :type type))))

(defn plane-rounded [name & {:keys [radius
                                    width
                                    height
                                    depth
                                    side-orientation
                                    billboard-mode
                                    visibility
                                    position
                                    rotation
                                    scale
                                    material
                                    type]
                             :or {radius 0.25
                                  width 1
                                  height 1
                                  depth 1
                                  side-orientation api.const/mesh-default-side
                                  type :plane-rounded}
                             :as opts}]
  (let [theta (/ Math/PI 32)
        pi Math/PI
        center-x (- (- (* 0.5 width) radius))
        center-z (- (- (* 0.5 height) radius))
        shape (for [theta (range pi (* 1.5 pi) theta)]
                (v3 (+ center-x (* radius (Math/cos theta))) 0 (+ center-z (* radius (Math/sin theta)))))
        center-x (- (* 0.5 width) radius)
        shape2 (for [theta (range (* 1.5 pi) (* 2 pi) theta)]
                 (v3 (+ center-x (* radius (Math/cos theta))) 0 (+ center-z (* radius (Math/sin theta)))))
        center-z (- (* 0.5 height) radius)
        shape3 (for [theta (range 0 (* 0.5 pi) theta)]
                 (v3 (+ center-x (* radius (Math/cos theta))) 0 (+ center-z (* radius (Math/sin theta)))))
        center-x (- (- (* 0.5 width) radius))
        shape4 (for [theta (range (* 0.5 pi) pi theta)]
                 (v3 (+ center-x (* radius (Math/cos theta))) 0 (+ center-z (* radius (Math/sin theta)))))
        shape (concat shape shape2 shape3 shape4)
        mesh (j/call MeshBuilder :CreatePolygon "polygon" (clj->js {:shape shape
                                                                    :sideOrientation side-orientation}) nil earcut)]
    (m/cond-doto mesh
      billboard-mode (j/assoc! :billboardMode (j/get Mesh billboard-mode))
      visibility (j/assoc! :visibility visibility)
      position (j/assoc! :position position)
      rotation (j/assoc! :rotation rotation)
      scale (j/assoc! :scaling scale)
      material (j/assoc! :material material))
    (j/assoc-in! mesh [:rotation :x] (/ Math/PI -2))
    (api.core/add-node-to-db name mesh (assoc opts :type type))))

(defn create-ground-from-hm [name & {:keys [texture subdivisions width height max-height min-height on-ready mat]
                                     :as opts}]
  (let [ground (j/call MeshBuilder :CreateGroundFromHeightMap name texture #js {:subdivisions subdivisions
                                                                                :width width
                                                                                :height height
                                                                                :maxHeight max-height
                                                                                :minHeight min-height
                                                                                :onReady on-ready})]
    (api.core/add-node-to-db name ground opts)
    (cond-> ground
      mat (j/assoc! :material mat))))

(defn create-ground [name & {:keys [width height mat pickable?]
                             :as opts}]
  (let [ground (j/call MeshBuilder :CreateGround name #js {:width width
                                                           :height height})]
    (api.core/add-node-to-db name ground opts)
    (cond-> ground
      mat (j/assoc! :material mat)
      (some? pickable?) (j/assoc! :isPickable pickable?))))

(defn create-hit-box [name mesh]
  (let [_ (j/call mesh :computeWorldMatrix true)
        bounding-info (j/call mesh :getBoundingInfo)
        extend-size (j/get-in bounding-info [:boundingBox :extendSize])
        hit-box (j/call MeshBuilder :CreateBox (str name "-hit-box") #js {:width (* 2 (j/get extend-size :x))
                                                                          :height (* 2 (j/get extend-size :y))
                                                                          :depth (* 2 (j/get extend-size :z))})]
    (j/assoc! hit-box
              :parent mesh
              :visibility 0
              :hit-box? true)
    (j/assoc-in! hit-box [:position :y] (j/get extend-size :y))
    hit-box))

;; TODO dispose material when disposing the mesh!!!
(defn text [name & {:keys [text
                           size
                           resolution
                           depth
                           visibility
                           position
                           rotation
                           color
                           emissive-color
                           emissive-intensity
                           metallic
                           roughness
                           alpha
                           mat
                           mat-type
                           face-to-screen?
                           hl-color
                           hl-blur
                           nme]
                    :or {size 1
                         depth 0.01
                         resolution 8
                         mat (api.material/pbr-mat (str name "-mat"))
                         mat-type :pbr
                         hl-blur 1.0}
                    :as opts}]
  (let [mesh (j/call MeshBuilder :CreateText name
                     text
                     font/droid
                     #js {:size 1
                          :resolution resolution
                          :depth 0.01
                          :sideOrientation api.const/mesh-default-side}
                     nil
                     earcut)
        mat (or (some-> nme api.material/get-nme-material) mat)]
    (create-hit-box name mesh)
    (when hl-color
      (let [hl (api.core/highlight-layer (str name "-hl")
                                         :blur-vertical-size hl-blur
                                         :blur-horizontal-size hl-blur)]
        (j/call hl :addMesh mesh hl-color)))
    (api.core/add-node-to-db name mesh (assoc opts :type :text3D))
    (when (= mat-type :pbr)
      (cond-> mat
        true (j/assoc! :reflectivityColor (api.const/color-black))
        ;; We're using the albedo color as the main color and emissive color as the brightness
        color (j/assoc! :albedoColor color)
        color (j/assoc! :emissiveColor color)
        emissive-color (j/assoc! :emissiveColor emissive-color)
        emissive-intensity (j/assoc! :emissiveIntensity emissive-intensity)
        roughness (j/assoc! :roughness roughness)
        metallic (j/assoc! :metallic metallic)
        alpha (j/assoc! :alpha alpha)))
    (cond-> mesh
      face-to-screen? (j/assoc! :billboardMode api.const/mesh-billboard-mode-all)
      mat (j/assoc! :material mat)
      visibility (j/assoc! :visibility visibility)
      position (j/assoc! :position position)
      rotation (j/assoc! :rotation rotation))
    (j/assoc! mesh
              :initial-rotation (api.core/clone (j/get mesh :rotation))
              :scaling (v3 size size (* 100 depth)))))

(comment

  (text "test" {:text "Text2"
                :depth 0.1
                ;:emissive-color (api.const/color-teal)
                :size 1
                ;:visibility 0.001
                :mat (api.material/pbr-mat "pbr"
                                           ;:alpha 0
                                           :albedo-color (api.const/color-red)
                                           ;:emissive-color (api.const/color-teal)
                                           ;:emissive-intensity 0
                                           :metallic 1
                                           :roughness 1
                                           )
                })

  )

(defn line [name & {:keys [points]
                    :as opts}]
  (let [l (j/call MeshBuilder :CreateLines name (clj->js {:points points}))]
    (api.core/add-node-to-db name l (assoc opts :type :line))))

(defn glb->mesh [name & {:keys [path
                                position
                                rotation
                                scale
                                update-materials]}]
  (let [model (api.core/clone (j/get-in api.core/db [:models path]))]
    (api.core/add-node-to-db name model {:type :glb})
    ;; TODO this apply to all meshes in the glb,it is not per mesh! fix it
    (doseq [[name {:keys [albedo-color use-alpha-from-albedo?]}] update-materials]
      (let [mat (api.core/get-mat-by-name name)]
        (m/cond-doto mat
          albedo-color (j/assoc! :albedoColor albedo-color)
          (some? use-alpha-from-albedo?) (j/assoc! :useAlphaFromAlbedoTexture use-alpha-from-albedo?))))
    (m/cond-doto model
      position (j/assoc! :position position)
      rotation (j/assoc! :rotation rotation)
      scale (j/assoc! :scaling scale))
    (j/assoc! model :initial-rotation (api.core/clone (j/get model :rotation)))
    (api.core/set-enabled model true)
    model))

(comment
  (j/assoc! (glb->mesh "porche" :path "model/porche_911.glb") :scaling (v3 1))
  )

(defn- get-points-from-text [& {:keys [text size resolution font]}]
  (j/call GreasedLineTools :GetPointsFromText text size resolution font))

(defn greased-line [name & {:keys [text
                                   position
                                   rotation
                                   visibility
                                   size
                                   resolution
                                   font
                                   material-type
                                   color-mode
                                   width
                                   color]
                            :or {width 0.1
                                 size 1
                                 resolution 4
                                 font font/droid
                                 color (api.const/color-white)}
                            :as opts}]
  (let [points (get-points-from-text :text text
                                     :size size
                                     :resolution resolution
                                     :font font)
        gl (CreateGreasedLine. name
                               #js {:points points}
                               #js {:color color
                                    :colorMode color-mode
                                    :materialType material-type
                                    :width width}
                               (api.core/get-scene))]
    (m/cond-doto gl
      position (j/assoc! :position position)
      rotation (j/assoc! :rotation rotation)
      visibility (j/assoc! :visibility visibility))
    (api.core/add-node-to-db name gl (assoc opts :type :greased-line))))

(comment
  (api.core/dispose "sphere-text-2" "sphere-text-3" "test")
  (js/console.log CreateGreasedLine)
  (greased-line "sphere-text-2"
                {:type :greased-line
                 :text "Over 30 ready-made materials"
                 :color (api.const/color-white)
                 :position (v3 -0.5 2 5)
                 :rotation (v3 (/ Math/PI 2) 0 0)
                 :width 0.01
                 :size 0.27
                 :visibility 1})

  (let [gl (greased-line "sphere-text-3"
                         {:type :greased-line
                          :text "for enhancing texts and models"
                          ;:material-type api.const/greased-line-material-pbr
                          ;:color-mode api.const/greased-line-color-mode-multi
                          :color (api.const/color-white)
                          :position (v3 -0.25 1.6 5)
                          :width 0.01
                          :size 0.23
                          :visibility 1})]
    )
  )
