(ns immersa.scene.api.mesh
  (:require
    ["@babylonjs/core/Meshes/mesh" :refer [Mesh]]
    ["@babylonjs/core/Meshes/meshBuilder" :refer [MeshBuilder]]
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
    (api.core/add-node-to-db name b (assoc opts :type (if skybox?
                                                        :skybox
                                                        :box)))
    (cond-> b
      mat (j/assoc! :material mat)
      position (j/assoc! :position position)
      visibility (j/assoc! :visibility visibility)
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
                             infinite-distance?]
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
    (api.core/add-node-to-db name s (assoc opts :type (if skybox?
                                                        :skybox
                                                        :sphere)))
    (m/cond-doto s
      mat (j/assoc! :material mat)
      position (j/assoc! :position position)
      rotation (j/assoc! :rotation rotation)
      scale (api.core/scaling scale)
      visibility (j/assoc! :visibility visibility)
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
                            type]
                     :or {type :plane}
                     :as opts}]
  (let [p (j/call MeshBuilder :CreatePlane name #js {:width width
                                                     :height height})]
    (m/cond-doto p
      billboard-mode (j/assoc! :billboardMode (j/get Mesh billboard-mode))
      visibility (j/assoc! :visibility visibility)
      position (j/assoc! :position position)
      rotation (j/assoc! :rotation rotation)
      scale (api.core/scaling scale)
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
      scale (api.core/scaling scale)
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

(defn create-ground [name & {:keys [width height mat]
                             :as opts}]
  (let [ground (j/call MeshBuilder :CreateGround name #js {:width width
                                                           :height height})]
    (api.core/add-node-to-db name ground opts)
    (cond-> ground
      mat (j/assoc! :material mat))))

(defn text [name & {:keys [text
                           size
                           resolution
                           depth
                           visibility
                           position
                           rotation
                           emissive-color
                           mat
                           hl-color]
                    :or {size 1
                         resolution 8
                         depth 1.0
                         mat (api.material/standard-mat (str name "-mat"))}
                    :as opts}]
  (let [text (j/call MeshBuilder :CreateText name
                     text
                     font/droid
                     #js {:size size
                          :resolution resolution
                          :depth depth
                          :sideOrientation api.const/mesh-double-side}
                     nil
                     earcut)]
    (when hl-color
      (let [hl (api.core/highlight-layer (str name "-hl"))
            [r g b] hl-color]
        (j/call hl :addMesh text (api.core/color r g b))))
    (api.core/add-node-to-db name text (assoc opts :type :text3D))
    (cond-> mat
      emissive-color (j/assoc! :emissiveColor emissive-color))
    (cond-> text
      mat (j/assoc! :material mat)
      visibility (j/assoc! :visibility visibility)
      position (j/assoc! :position position)
      rotation (j/assoc! :rotation rotation))))

(defn glb->mesh [name & {:keys [path
                                position
                                rotation
                                scale]}]
  (let [m (api.core/clone (j/get-in api.core/db [:models path]))]
    (m/cond-doto m
      position (j/assoc! :position position)
      rotation (j/assoc! :rotation rotation)
      scale (api.core/scaling scale))
    (api.core/add-node-to-db name m {:type :glb})
    (api.core/set-enabled m true)))

(comment

  (glb->mesh "a" {:type :glb
                  :path "model/plane.glb"
                  :position (v3 1 1 1)
                  :rotation (v3 -0.25 Math/PI -0.4)
                  })

  (api.core/dispose "text-dots" "t2" "t" "2d-slide-text-2")
  (text "t"
        {:type :text3D
         :text "From 2D clarity to..."
         :depth 0.001
         :emissive-color api.const/color-white
         :size 0.215
         :position (v3 0 2.65 5)
         :visibility 1})

  (text "t2"
        {:type :text3D
         :text "3D Immersion with Immersa"
         :depth 0.1
         ;:emissive-color api.const/color-white
         :size 0.2
         :position (v3 0 2.65 5)
         :hl-color [1 1 1]
         ;:rotation (v3 (/ js/Math.PI 2) 0 0)
         :visibility 1})
  )
