(ns immersa.scene.core
  (:require
    [applied-science.js-interop :as j]
    [cljs.core.async :as a :refer [go go-loop <!]]
    [immersa.common.utils :as common.utils]
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

(defn create-focus-camera-anim [object-slide-info]
  (when-let [object-name (:focus object-slide-info)]
    (let [_ (api/update-active-camera)
          object (api/get-object-by-name object-name)
          focus-type (:type object-slide-info)
          _ (j/call object :computeWorldMatrix true)
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
                                                 {:frame 30 :value (case focus-type
                                                                     :left (v3 (+ x (* diagonal-size radius 0.35)) y z)
                                                                     :right (v3 (- x (* diagonal-size radius 0.35)) y z)
                                                                     (v3 x y z))}]
                                          :easing easing-function)]
      [:camera [position-animation target-animation]])))

(defn create-position-animation [start end duration]
  (api/animation "position-animation"
                 :target-prop "position"
                 :fps 30
                 :data-type :ANIMATIONTYPE_VECTOR3
                 :loop-mode :ANIMATIONLOOPMODE_CONSTANT
                 :keys [{:frame 0 :value start}
                        {:frame (* 30 duration) :value end}]
                 :easing (api/cubic-ease :EASINGMODE_EASEINOUT)))

(defn create-rotation-animation [start end duration]
  (api/animation "rotation-animation"
                 :target-prop "rotation"
                 :fps 30
                 :data-type :ANIMATIONTYPE_VECTOR3
                 :loop-mode :ANIMATIONLOOPMODE_CONSTANT
                 :keys [{:frame 0 :value start}
                        {:frame (* 30 duration) :value end}]
                 :easing (api/cubic-ease :EASINGMODE_EASEINOUT)))

(defn- create-visibility-animation [start end duration]
  (api/animation "visibility-animation"
                 :target-prop "visibility"
                 :fps 30
                 :data-type :ANIMATIONTYPE_FLOAT
                 :loop-mode :ANIMATIONLOOPMODE_CONSTANT
                 :keys [{:frame 0 :value start}
                        {:frame (* 30 duration) :value end}]
                 :easing (api/cubic-ease :EASINGMODE_EASEINOUT)))

(defn- get-position-anim [object-slide-info object-name]
  (let [last-pos (:position object-slide-info)
        init-pos (some-> (api/get-object-by-name object-name) (j/get :position))]
    (when (and init-pos last-pos (not (api/equals? init-pos last-pos)))
      [object-name (create-position-animation init-pos last-pos 1)])))

(defn- get-rotation-anim [object-slide-info object-name]
  (let [last-rotation (:rotation object-slide-info)
        init-rotation (some-> (api/get-object-by-name object-name) (j/get :rotation))]
    (when (and init-rotation last-rotation (not (api/equals? init-rotation last-rotation)))
      [object-name (create-rotation-animation init-rotation last-rotation 1)])))

(defn- get-visibility-anim [object-slide-info object-name]
  (let [last-visibility (:visibility object-slide-info)
        init-visibility (j/get (api/get-object-by-name object-name) :visibility)]
    (when (and last-visibility (not= init-visibility last-visibility))
      [object-name (create-visibility-animation init-visibility last-visibility 1)])))

(defn- get-animations-from-slide-info [acc object-slide-info object-name]
  (reduce
    (fn [acc anim-type]
      (let [[object-name anim :as anim-vec] (case anim-type
                                              :position (get-position-anim object-slide-info object-name)
                                              :rotation (get-rotation-anim object-slide-info object-name)
                                              :visibility (get-visibility-anim object-slide-info object-name)
                                              :focus (create-focus-camera-anim object-slide-info))]
        (cond-> acc
                (and (not= object-name :camera) anim-vec)
                (conj anim-vec)

                (and (= object-name :camera) anim-vec)
                (conj [object-name (first anim)] [object-name (second anim)]))))
    acc
    [:position :rotation :visibility :focus]))

(comment

  (a/go-loop [slides [{:objects {"box" {:type :box
                                        :position (v3 0 -5 0)}
                                 "box2" {:type :box
                                         :position (v3 2 0 0)}}
                       :data {"box" {:position (v3 0 0 0)
                                     :rotation (v3 0 2.4 0)
                                     :visibility 0.5}}}
                      {:data {"box" {:position (v3 -2 0 0)
                                       :on-animation-end (fn [obj]
                                                           (println "animation ended"))}}}
                      {:data {"box" {:position (v3 0 2 0)
                                       :rotation (v3 1.2 2.3 4.1)
                                       :visibility 1}}}
                      {:data {:camera {:focus "box"
                                       :type :left}}}]]
             (when-let [slide (first slides)]
               (let [objects (:objects slide)
                     object-names-from-slide-info (keys (:data slide))
                     object-names-from-objects (-> slide :objects keys)
                     objects-to-create (filter #(not (api/get-object-by-name %)) (concat object-names-from-slide-info
                                                                                         object-names-from-objects))
                     _ (doseq [name objects-to-create]
                         (let [params (get objects name)
                               type (:type params)
                               params (dissoc params :type)]
                           (case type
                             :box (create-box name params)
                             :text (api/text name params)
                             nil)))
                     animations (reduce
                                  (fn [acc object-name]
                                    (let [object-slide-info (get-in slide [:data object-name])]
                                      (get-animations-from-slide-info acc object-slide-info object-name)))
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

(defn reset-camera []
  (let [cam (api/active-camera)]
    (j/assoc! cam
              :position (api/clone (j/get cam :init-position))
              :rotation (api/clone (j/get cam :init-rotation)))))

(comment
  (api/dispose-all (api/get-objects-by-type "box"))
  (reset-camera)

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
        camera (api/create-free-camera "free-camera" :position (v3 0 0 -10))
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
