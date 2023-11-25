(ns immersa.scene.core
  (:require [applied-science.js-interop :as j]
            [immersa.scene.api :as api :refer [v3]]))

(defn start-scene [canvas]
  (let [engine (api/create-engine canvas)
        scene (api/create-scene engine)
        camera (api/create-free-camera "free-camera" (v3 0 2 -5))
        light (api/hemispheric-light "light")
        box  (api/box "box")
        ground (api/create-ground "ground" :width 6 :height 6)]
    (j/assoc! light :intensity 0.7)
    (j/call camera :setTarget (v3))
    (j/call camera :attachControl canvas false)
    (j/call engine :runRenderLoop #(j/call scene :render))))
