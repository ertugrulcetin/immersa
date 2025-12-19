(ns immersa.presentations.init
  "Initial presentation template for new users.")

(def slides
  "Default slides for a new presentation - uses local assets."
  [{:id "14e4ee76-bb27-4904-9d30-360a40d8abb7"
    :data {:camera {:position [0 5 -10]
                    :rotation [0.17453292519943295 0 0]
                    :initial-position [0 5 -10]
                    :initial-rotation [0.17453292519943295 0 0]
                    :locked? true}
           :skybox {:background {:color [87 97 166]
                                 :brightness 0.1}}
           :ground {:enabled? true}
           ;; Example 3D model using local asset
           "b112fe37-e51b-49e5-a152-4f3989f1c2f8" {:type :glb
                                                   :asset-type :model
                                                   :path "model/porche_911.glb"
                                                   :position [0 0.8 0]
                                                   :rotation [0 0 0]
                                                   :scale [1 1 1]}}}])

(def thumbnails
  "Initial thumbnails (empty for new presentations)."
  {})
