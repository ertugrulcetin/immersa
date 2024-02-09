(ns immersa.ui.db)

(def default-db
  {:name "re-frame"
   :mode :editor
   :editor {:slides {:current-index 0
                     :all [{:id "14e4ee76-bb27-4904-9d30-360a40d8abb7"
                            :data {:camera {:position [0 0 -10]
                                            :rotation [0 0 0]}
                                   :skybox {:background {:color [1000 1000 1000]}}
                                   "wave" {:type :wave}
                                   "33e4ee76-bb27-4904-9d30-360a40d8abc1" {:type :image
                                                                           :asset-type :texture
                                                                           :visibility 0
                                                                           :path "https://firebasestorage.googleapis.com/v0/b/immersa-6d29f.appspot.com/o/images%2Fschaltbau%2Flogo.png?alt=media&token=2afccb59-5489-4553-9a98-0425f0bac1db"
                                                                           :transparent? true
                                                                           :position [0 0.5 0]
                                                                           :rotation [0 0 0]
                                                                           :scale [1 1 1]}}}
                           {:id "23e4ee76-bb27-4904-9d30-360a40d8abb1"
                            :data {:camera {:position [0 0 -10]
                                            :rotation [0 0 0]}
                                   :skybox {:background {:color [1000 1000 1000]}}
                                   "wave" {:type :wave}
                                   "22e09fae-b39f-4901-9283-bc1cdb7374bb" {:type :particle
                                                                           :particle-type :cloud
                                                                           :position [0 -7 0]
                                                                           :scale 5
                                                                           :update-speed 0.01}
                                   "33e4ee76-bb27-4904-9d30-360a40d8abc1" {:type :image
                                                                           :asset-type :texture
                                                                           :visibility 1
                                                                           :path "https://firebasestorage.googleapis.com/v0/b/immersa-6d29f.appspot.com/o/images%2Fschaltbau%2Flogo.png?alt=media&token=2afccb59-5489-4553-9a98-0425f0bac1db"
                                                                           :transparent? true
                                                                           :position [0 0.5 0]
                                                                           :rotation [0 0 0]
                                                                           :scale [1 1 1]}}}
                           {:id "44e4ee76-bb27-4904-9d30-360a40d8abb1"
                            :data {:camera {:position [0 0 -10]
                                            :rotation [0 0 0]}
                                   :skybox {:background {:color [1000 1000 1000]}}
                                   "08e09fae-b39f-4901-9283-bc1cdb737490" {:rotation [0 0 0]
                                                                           :color [0 0 0]
                                                                           :scale [1 1 1]
                                                                           :roughness 1
                                                                           :metallic 0
                                                                           :type :text3D
                                                                           :size 1
                                                                           :position [0 0 11.4]
                                                                           :depth 0.01
                                                                           :visibility 1
                                                                           :emissive-intensity 1
                                                                           :text "1929’dan bu yana en yüksek güvenlik standartı"}
                                   "22e09fae-b39f-4901-9283-bc1cdb7374bb" {:type :particle
                                                                           :particle-type :cloud
                                                                           :position [0 -0.5 0]
                                                                           :scale 5
                                                                           :update-speed 0.01}
                                   "33e4ee76-bb27-4904-9d30-360a40d8abc1" {:type :image
                                                                           :asset-type :texture
                                                                           :visibility 1
                                                                           :path "https://firebasestorage.googleapis.com/v0/b/immersa-6d29f.appspot.com/o/images%2Fschaltbau%2Flogo.png?alt=media&token=2afccb59-5489-4553-9a98-0425f0bac1db"
                                                                           :transparent? true
                                                                           :position [-6.2 3.73 0],
                                                                           :rotation [0 0 0],
                                                                           :scale [0.5 0.5 0.5]}}}
                           {:id "10e4ee76-bb27-4904-9d30-360a40d8abb0"
                            :data {:camera {:position [0 0 -10]
                                            :rotation [0 0 0]}
                                   :skybox {:background {:color [1000 1000 1000]}}
                                   "1a8ac149-35cc-402d-8bde-a2287261930e" {:rotation [0 -0.0003164062503844267 0]
                                                                           :color [0 0 0]
                                                                           :scale [1 1 1]
                                                                           :roughness 1
                                                                           :metallic 0
                                                                           :type :text3D
                                                                           :size 1
                                                                           :position [0 -3.48 14.6]
                                                                           :depth 0.01
                                                                           :visibility 1
                                                                           :emissive-intensity 1
                                                                           :text "• Konnektörler \n• Ani hareket anahtarları (Snap action switch)\n• AC ve DC kontaktörler\n• Demiryolu bileşenleri "}
                                   "22e09fae-b39f-4901-9283-bc1cdb7374bb" {:type :particle
                                                                           :particle-type :cloud
                                                                           :position [0 -7 0]
                                                                           :scale 5
                                                                           :update-speed 0.01}
                                   "33e4ee76-bb27-4904-9d30-360a40d8abc1" {:type :image
                                                                           :asset-type :texture
                                                                           :visibility 1
                                                                           :path "https://firebasestorage.googleapis.com/v0/b/immersa-6d29f.appspot.com/o/images%2Fschaltbau%2Flogo.png?alt=media&token=2afccb59-5489-4553-9a98-0425f0bac1db"
                                                                           :transparent? true
                                                                           :position [-6.2 3.73 0],
                                                                           :rotation [0 0 0],
                                                                           :scale [0.5 0.5 0.5]}
                                   "55k4ee76-bb27-4904-9d30-360a40d8ab00" {:type :glb
                                                                           :asset-type :model
                                                                           :path "https://firebasestorage.googleapis.com/v0/b/immersa-6d29f.appspot.com/o/models%2Fschaltbau%2Ftrain.glb?alt=media&token=5059924a-b432-4c9a-9711-3c401259083b"
                                                                           :position [-64 2 75]
                                                                           :rotation [0 2.44 0]
                                                                           :scale [0.001 0.001 0.001]}}}
                           {:id "3bc5da96-f729-4ca1-a5e4-ab22fecd29b7"
                            :data {:camera {:position [0 3 -10]
                                            :rotation [0 0 0]}
                                   :skybox {:background {:color [255 255 255]}}
                                   "55k4ee76-bb27-4904-9d30-360a40d8ab00" {:type :glb
                                                                           :asset-type :model
                                                                           :path "https://firebasestorage.googleapis.com/v0/b/immersa-6d29f.appspot.com/o/models%2Fschaltbau%2Ftrain.glb?alt=media&token=5059924a-b432-4c9a-9711-3c401259083b"
                                                                           :position [-9 2 11]
                                                                           :rotation [0 2.44 0]
                                                                           :scale [0.001 0.001 0.001]}}}]}}
   :present {:show-arrow-keys-text? true
             :show-pre-warm-text? false
             :background-color "rgb(0,0,0)"}})
