(ns immersa.scene.core
  (:require
    [applied-science.js-interop :as j]
    [cljs.core.async :as a :refer [go go-loop <!]]
    [immersa.common.utils :as common.utils]
    [immersa.scene.api :as api :refer [v3 v4]]))

(defn reset-camera []
  (let [cam (api/active-camera)]
    (j/assoc! cam
              :position (api/clone (j/get cam :init-position))
              :rotation (api/clone (j/get cam :init-rotation)))))

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
    (let [object (api/get-object-by-name object-name)
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
          easing-function (api/cubic-ease api/easing-ease-in-out)
          position-animation (api/animation "camera-position-anim"
                                            :target-prop "position"
                                            :fps 30
                                            :data-type api/animation-type-v3
                                            :loop-mode api/animation-loop-cons
                                            :keys [{:frame 0 :value (j/get camera :position)}
                                                   {:frame 30 :value final-position}]
                                            :easing easing-function)
          target-animation (api/animation "camera-target-anim"
                                          :target-prop "target"
                                          :fps 30
                                          :data-type api/animation-type-v3
                                          :loop-mode api/animation-loop-cons
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
                 :data-type api/animation-type-v3
                 :loop-mode api/animation-loop-cons
                 :keys [{:frame 0 :value start}
                        {:frame (* 30 duration) :value end}]
                 :easing (api/cubic-ease api/easing-ease-in-out)))

(defn create-rotation-animation [start end duration]
  (api/animation "rotation-animation"
                 :target-prop "rotation"
                 :fps 30
                 :data-type api/animation-type-v3
                 :loop-mode api/animation-loop-cons
                 :keys [{:frame 0 :value start}
                        {:frame (* 30 duration) :value end}]
                 :easing (api/cubic-ease api/easing-ease-in-out)))

(defn- create-visibility-animation [start end duration]
  (api/animation "visibility-animation"
                 :target-prop "visibility"
                 :fps 30
                 :data-type :ANIMATIONTYPE_FLOAT
                 :loop-mode api/animation-loop-cons
                 :keys [{:frame 0 :value start}
                        {:frame (* 30 duration) :value end}]
                 :easing (api/cubic-ease api/easing-ease-in-out)))

(defn- get-position-anim [object-slide-info object-name]
  (let [object (api/get-object-by-name object-name)
        start-pos (j/get object :position)
        end-pos (:position object-slide-info)]
    (cond
      (and start-pos end-pos (not (api/equals? start-pos end-pos)))
      [object-name (create-position-animation start-pos end-pos 1)]

      (and (= object-name :camera) (not (api/equals? start-pos (j/get object :init-position))))
      [object-name (create-position-animation start-pos (api/clone (j/get object :init-position)) 1)])))

(defn- get-rotation-anim [object-slide-info object-name]
  (let [object (api/get-object-by-name object-name)
        start-rotation (j/get object :rotation)
        end-rotation (:rotation object-slide-info)]
    (cond
      (and start-rotation end-rotation (not (api/equals? start-rotation end-rotation)))
      [object-name (create-rotation-animation start-rotation end-rotation 1)]

      (and (= object-name :camera) (not (api/equals? start-rotation (j/get object :init-rotation))))
      [object-name (create-rotation-animation start-rotation (api/clone (j/get object :init-rotation)) 1)])))

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
          (and (not= anim-type :focus) anim-vec)
          (conj anim-vec)

          (and (= anim-type :focus) anim-vec)
          (conj [object-name (first anim)] [object-name (second anim)]))))
    acc
    [:position :rotation :visibility :focus]))

(def slides
  [{:objects {"box" {:type :box
                     :position (v3 0 -5 0)}
              "box2" {:type :box
                      :position (v3 2 0 0)}}
    :data {"box" {:position (v3 0 0 0)
                  :rotation (v3 0 2.4 0)
                  :visibility 0.5}}}
   {:data {"box" {:position (v3 -2 0 0)}}}
   {:data {"box" {:position (v3 0 2 0)
                  :rotation (v3 1.2 2.3 4.1)
                  :visibility 1}}}
   {:data {:camera {:focus "box"
                    :type :left}}}
   {:data {:camera {:focus "box"
                    :type :right}}}
   {:data {:camera {:position (v3 0 0 -10)
                    :rotation (v3 0 0 0)}}}])

(comment

  (let [command-ch (a/chan)]
    (api/dispose-all (api/get-objects-by-type "box"))
    (reset-camera)
    (api/detach-control (api/active-camera))
    (common.utils/remove-element-listeners)

    (common.utils/register-event-listener js/window "keydown"
                                          (fn [e]
                                            (when-not (j/get e :repeat)
                                              (cond
                                                (= (.-keyCode e) 39) (a/put! command-ch :next)
                                                (= (.-keyCode e) 37) (a/put! command-ch :prev)))))
    (a/go-loop [index -1]
               (let [command (a/<! command-ch)
                     next-index (case command
                                  :next (inc index)
                                  :prev (dec index))]
                 (if (and (>= next-index 0) (< next-index (count slides)))
                   (let [slide (slides next-index)
                         objects (:objects slide)
                         object-names-from-slide-info (set (conj (keys (:data slide)) :camera))
                         _ (when (object-names-from-slide-info :camera)
                             (api/update-active-camera))
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
                                               ;;TODO get the longest animation and use it as the duration (:to)
                                               (assoc acc name {:target (api/get-object-by-name name)
                                                                :animations (mapv second animations)
                                                                :from 0
                                                                :to 30}))
                                             {}
                                             (group-by first animations)))
                         channels (mapv #(api/begin-direct-animation %) animations-data)]
                     (doseq [c channels]
                       (a/<! c))
                     (recur next-index))
                   (recur index))))))

(comment
  (do
    (api/dispose-all (api/get-objects-by-type "box"))
    (reset-camera))

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
                                 :loop-mode api/animation-loop-cons
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
