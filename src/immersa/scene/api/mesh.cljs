(ns immersa.scene.api.mesh
  (:require
    ["@babylonjs/core/Meshes/mesh" :refer [Mesh]]
    ["@babylonjs/core/Meshes/meshBuilder" :refer [MeshBuilder]]
    ["earcut" :as earcut]
    [applied-science.js-interop :as j]
    [immersa.scene.api.core :as api.core]
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
    (api.core/add-node-to-db name s opts)
    (m/cond-doto s
      mat (j/assoc! :material mat)
      position (j/assoc! :position position)
      rotation (j/assoc! :rotation rotation)
      scale (api.core/scaling scale)
      visibility (j/assoc! :visibility visibility))))

(defn capsule [name & {:keys [height radius visibility]
                       :as opts}]
  (let [c (j/call MeshBuilder :CreateCapsule name #js {:height height
                                                       :radius radius})]
    (api.core/add-node-to-db name c opts)
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
      scale (api.core/scaling scale))
    (api.core/add-node-to-db name p opts)))

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
    (api.core/add-node-to-db name text (assoc opts :type :text3D))
    (cond-> text
      visibility (j/assoc! :visibility visibility)
      position (j/assoc! :position position))))
