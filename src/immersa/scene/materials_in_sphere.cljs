(ns immersa.scene.materials-in-sphere
  (:require [applied-science.js-interop :as j]
            [immersa.scene.api.camera :as api.camera]
            [immersa.scene.api.core :as api.core :refer [v3]]
            [immersa.scene.api.material :as api.material]
            [immersa.scene.api.mesh :as api.mesh]))

(defn- sphere-specular [name opts]
  (api.mesh/sphere
    name
    (assoc opts
      :mat (api.material/pbr-metallic-roughness-mat
             "pbr"
             :base-color (api.core/color 1.0 0.766 0.336)
             :metallic 1.0
             :roughness 0.0
             :environment-texture (api.core/create-from-prefiltered-data "img/texture/environment/environmentSpecular.env")))))

(defn- sphere-stone [name opts]
  (api.mesh/sphere
    name
    (assoc opts :mat (let [mat (api.material/get-nme-material :sphere-stone)
                           stone1 (api.core/texture "img/texture/sphere_stone_1.png")
                           stone2 (api.core/texture "img/texture/sphere_stone_2.png")
                           stone3 (api.core/texture "img/texture/sphere_stone_3.png")
                           stone4 (api.core/texture "img/texture/sphere_stone_4.png")]
                       (j/assoc! (api.material/get-block-by-name mat "sphere1") :texture stone1)
                       (j/assoc! (api.material/get-block-by-name mat "sphere2") :texture stone2)
                       (j/assoc! (api.material/get-block-by-name mat "sphere22") :texture (api.core/clone stone2))
                       (j/assoc! (api.material/get-block-by-name mat "sphere3") :texture stone3)
                       (j/assoc! (api.material/get-block-by-name mat "sphere33") :texture (api.core/clone stone3))
                       (j/assoc! (api.material/get-block-by-name mat "sphere4") :texture stone4)
                       mat))))

(defn- sphere-copper [name opts]
  (api.mesh/sphere name
                   (assoc opts :mat
                               (api.material/pbr-mat
                                 (str name "-mat")
                                 :reflection-texture (api.core/cube-texture :root-url "img/texture/environment/cube_specular.env")
                                 :albedo-texture (api.core/texture "img/texture/copper-diffuse.png")
                                 :bump-texture (api.core/texture "img/texture/copper-normal.jpg")
                                 :metallic 1
                                 :roughness 1))))

(defn- sphere-nebula [name opts]
  (api.mesh/sphere name (assoc opts :mat (api.material/get-nme-material :nebula))))

(defn- sphere-world [name opts]
  (api.mesh/sphere name (assoc opts :mat (api.material/get-nme-material :sphere-world))))

(defn- sphere-translucent [name opts]
  (api.mesh/sphere name (assoc opts :mat (api.material/get-nme-material :translucent))))

(defn get-sphere [name & {:keys [component] :as opts}]
  (let [f (case component
            :specular sphere-specular
            :stone sphere-stone
            :copper sphere-copper
            :nebula sphere-nebula
            :world sphere-world
            :translucent sphere-translucent)]
    (f name (assoc opts :type :sphere-mat))))

(comment
  (api.camera/set-pos (v3 0 2 -1))
  (api.camera/reset-camera)
  (api.core/dispose "c")
  (j/assoc! (sphere-specular "a" :position (v3 0 0.75 4)) :visibility 1)
  (j/assoc! (sphere-stone "b" :position (v3 4.5 0.75 4)) :visibility 1)
  (j/assoc! (sphere-copper "c" :position (v3 -2 0.75 4)) :visibility 1)

  (j/assoc! (sphere-nebula "d" :position (v3 0 2.25 4)) :visibility 1)
  (j/assoc! (sphere-world "e" :position (v3 2 2.25 4)) :visibility 1)
  (j/assoc! (sphere-translucent "f" :position (v3 -2 2.25 4)) :visibility 1)

  (get-sphere "s"
              :component :specular
              :position (api.core/v3 0 -1 0))
  )