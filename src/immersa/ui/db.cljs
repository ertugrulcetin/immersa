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
                                   :skybox {:background {:color [500 500 500]}}
                                   "33e4ee76-bb27-4904-9d30-360a40d8abc1" {:type :image
                                                                           :asset-type :texture
                                                                           :visibility 1
                                                                           :path "https://firebasestorage.googleapis.com/v0/b/immersa-6d29f.appspot.com/o/images%2Fschaltbau%2Flogo.png?alt=media&token=2afccb59-5489-4553-9a98-0425f0bac1db"
                                                                           :transparent? true
                                                                           :position [-6.2 3.73 0],
                                                                           :rotation [0 0 0],
                                                                           :scale [0.5 0.5 0.5]}}}
                           {:id "3bc5da96-f729-4ca1-a5e4-ab22fecd29b7"
                            :data {:camera {:position [0 5 -10]
                                            :rotation [0 0 0]}
                                   :skybox {:background {:color [255 255 255]}}}}]}}
   :present {:show-arrow-keys-text? true
             :show-pre-warm-text? false
             :background-color "rgb(0,0,0)"}})
