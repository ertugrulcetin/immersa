(ns immersa.scene.core
  (:require
    [applied-science.js-interop :as j]
    [cljs.core.async :as a :refer [go go-loop <!]]
    [immersa.scene.api :as api :refer [v3 v4]]))

(defn- create-box [name params]
  (let [columns 6
        rows 1
        face-uv (js/Array. columns)
        _ (dotimes [i columns]
            (aset face-uv i (v4 (/ i columns) 0 (/ (+ i 1) columns) (/ 1 rows))))
        texture (api/texture "img/texture/numbers.jpg")
        mat (api/standard-mat "mat" :diffuse-texture texture)]
    (api/box name
             (assoc params
                    :face-uv face-uv
                    :wrap? true
                    :mat mat))))

(defn animate-camera [object]
  (let [_ (j/call object :computeWorldMatrix true)
        bounding-box (j/get (j/call object :getBoundingInfo) :boundingBox)
        diagonal-size (j/call-in bounding-box [:maximumWorld :subtract] (j/get bounding-box :minimumWorld))
        diagonal-size (j/call diagonal-size :length)
        camera (api/active-camera)
        {:keys [x y z]} (j/lookup (api/get-pos object))
        radius 1.5
        final-radius (* radius diagonal-size)
        final-position (v3 x y (- z final-radius))
        easing-function (api/cubic-ease :EASINGMODE_EASEINOUT)
        position-animation (api/animation "cameraPositionAnim"
                                          :target-prop "position"
                                          :fps 30
                                          :data-type :ANIMATIONTYPE_VECTOR3
                                          :loop-mode :ANIMATIONLOOPMODE_CONSTANT
                                          :keys [{:frame 0 :value (j/get camera :position)}
                                                 {:frame 30 :value final-position}]
                                          :easing easing-function)
        target-animation (api/animation "cameraTargetAnim"
                                        :target-prop "target"
                                        :fps 15
                                        :data-type :ANIMATIONTYPE_VECTOR3
                                        :loop-mode :ANIMATIONLOOPMODE_CONSTANT
                                        :keys [{:frame 0 :value (j/call camera :getTarget)}
                                               {:frame 30 :value (v3 (+ x (* diagonal-size radius 0.35)) y z)}]
                                        :easing easing-function)]
    (api/begin-direct-animation :target camera
                                :animations [position-animation target-animation]
                                :from 0
                                :to 30
                                :delay 1000)))

(defn create-position-animation [start end duration]
  (api/animation "position-animation"
                 :target-prop "position"
                 :fps 30
                 :data-type :ANIMATIONTYPE_VECTOR3
                 :loop-mode :ANIMATIONLOOPMODE_CONSTANT
                 :keys [{:frame 0 :value start}
                        {:frame (* 30 duration) :value end}]
                 :easing (api/cubic-ease :EASINGMODE_EASEINOUT)))

(comment
  (a/go-loop [slides [{:objects {"box" {:type :box
                                       :position (v3 0 -5 0)}
                                "box2" {:type :box
                                        :position (v3 2 0 0)}}
                      "box" {:position (v3 0 0 0)}}
                      {"box" {:position (v3 -2 0 0)}}
                      {"box" {:position (v3 0 2 0)}}]]
             (when-let [slide (first slides)]
               (let [objects (:objects slide)
                    object-names-from-slide-info (keys (dissoc slide :objects))
                    object-names-from-objects (-> slide :objects keys)
                    objects-to-create (filter #(not (api/get-object-by-name %)) (concat object-names-from-slide-info
                                                                                        object-names-from-objects))
                    _ (doseq [name objects-to-create]
                        (let [params (get objects name)
                              type (:type params)
                              params (dissoc params :type)]
                          (case type
                            :box (create-box name params))))
                    animations (reduce
                                 (fn [acc object-name]
                                   (let [object-slide-info (get slide object-name)]
                                     (if-let [last-pos (:position object-slide-info)]
                                       (if-let [init-pos (api/get-pos (api/get-object-by-name object-name))]
                                         (if-not (api/equals? init-pos last-pos)
                                           (conj acc [object-name (create-position-animation init-pos last-pos 1)])
                                           acc)
                                         acc)
                                       acc)))
                                 []
                                 object-names-from-slide-info)
                    animations-data (vals
                                      (reduce-kv
                                        (fn [acc name animations]
                                          (assoc acc name {:target (api/get-object-by-name name)
                                                           :animations (mapv second animations)
                                                           :from 0
                                                           :to 30
                                                           :delay 1000}))
                                        {}
                                        (group-by first animations)))
                     channels (mapv #(api/begin-direct-animation %) animations-data)]
                 (doseq [c channels]
                   (a/<! c))
                 (recur (rest slides))))))

(comment
  (api/dispose-all (api/get-objects-by-type "box"))

  (api/dispose "box2")
  (do
    (api/dispose "box")
    (create-box "box" {:position (v3 2 0 0)}))

  (api/show-debug)

  (let [box (api/get-object-by-name "box")
        keys [{:frame 0 :value 0}
              {:frame 30 :value 5}]
        animation (api/animation "linearAnimation"
                                 :target-prop "position.y"
                                 :fps 30
                                 :data-type :ANIMATIONTYPE_FLOAT
                                 :loop-mode :ANIMATIONLOOPMODE_CONSTANT
                                 :keys keys)]
    (api/begin-direct-animation :target box
                                :animations animation
                                :from 0
                                :to 30
                                :delay 1500)))

(defn when-scene-ready [])

(defn start-scene [canvas]
  (let [engine (api/create-engine canvas)
        scene (api/create-scene engine)
        camera (api/create-free-camera "free-camera" :pos (v3 0 0 -10))
        light (api/hemispheric-light "light")
        ground-material (api/grid-mat "grid-mat"
                                      :major-unit-frequency 5
                                      :minor-unit-visibility 0.45
                                      :grid-ratio 2
                                      :back-face-culling? false
                                      :main-color (api/color 1 1 1)
                                      :line-color (api/color 1 1 1)
                                      :opacity 0.98)
        ground (api/create-ground "ground"
                                  :width 50
                                  :height 50
                                  :mat ground-material)
        sky-box (api/create-sky-box)]
    (j/assoc! light :intensity 0.7)
    (j/call camera :setTarget (v3))
    (j/call camera :attachControl canvas false)
    (j/call engine :runRenderLoop #(j/call scene :render))
    (j/call scene :executeWhenReady when-scene-ready)))
