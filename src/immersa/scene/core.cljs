(ns immersa.scene.core
  (:require
    [applied-science.js-interop :as j]
    [cljs.core.async :as a :refer [go go-loop <!]]
    [clojure.set :as set]
    [immersa.common.utils :as common.utils]
    [immersa.events :as events]
    [immersa.scene.api.animation :as api.animation]
    [immersa.scene.api.camera :as api.camera]
    [immersa.scene.api.component :as api.component]
    [immersa.scene.api.constant :as api.const]
    [immersa.scene.api.core :as api.core :refer [v3 v4]]
    [immersa.scene.api.gui :as api.gui]
    [immersa.scene.api.light :as api.light]
    [immersa.scene.api.material :as api.material]
    [immersa.scene.api.mesh :as api.mesh]
    [re-frame.core :refer [dispatch]]))

(defn- get-position-anim [object-slide-info object-name]
  (let [object (api.core/get-object-by-name object-name)
        start-pos (j/get object :position)
        end-pos (:position object-slide-info)]
    (cond
      (and start-pos end-pos (not (api.core/equals? start-pos end-pos)))
      [object-name (api.animation/create-position-animation {:start start-pos
                                                             :end end-pos})]

      (and (= object-name :camera) (not (api.core/equals? start-pos (j/get object :init-position))))
      [object-name (api.animation/create-position-animation {:start start-pos
                                                             :end (api.core/clone (j/get object :init-position))})])))

(defn- get-rotation-anim [object-slide-info object-name]
  (let [object (api.core/get-object-by-name object-name)
        start-rotation (j/get object :rotation)
        end-rotation (:rotation object-slide-info)]
    (cond
      (and start-rotation end-rotation (not (api.core/equals? start-rotation end-rotation)))
      [object-name (api.animation/create-rotation-animation {:start start-rotation
                                                             :end end-rotation})]

      (and (= object-name :camera) (not (api.core/equals? start-rotation (j/get object :init-rotation))))
      [object-name (api.animation/create-rotation-animation {:start start-rotation
                                                             :end (api.core/clone (j/get object :init-rotation))})])))

(defn- get-visibility-anim [object-slide-info object-name]
  (let [end-visibility (:visibility object-slide-info)
        start-visibility (j/get (api.core/get-object-by-name object-name) :visibility)]
    (when (and end-visibility (not= start-visibility end-visibility))
      [object-name (api.animation/create-visibility-animation {:start start-visibility
                                                               :end end-visibility})])))

(defn- get-alpha-anim [object-slide-info object-name]
  (let [end-alpha (:alpha object-slide-info)
        start-alpha (j/get (api.core/get-object-by-name object-name) :alpha)]
    (when (and end-alpha (not= start-alpha end-alpha))
      [object-name (api.animation/create-alpha-animation {:start start-alpha
                                                          :end end-alpha})])))

(defn- get-animations-from-slide-info [acc object-slide-info object-name]
  (reduce
    (fn [acc anim-type]
      (let [[object-name anim :as anim-vec] (case anim-type
                                              :position (get-position-anim object-slide-info object-name)
                                              :rotation (get-rotation-anim object-slide-info object-name)
                                              :visibility (get-visibility-anim object-slide-info object-name)
                                              :alpha (get-alpha-anim object-slide-info object-name)
                                              :focus (api.animation/create-focus-camera-anim object-slide-info))]
        (cond-> acc
          (and (not= anim-type :focus) anim-vec)
          (conj anim-vec)

          (and (= anim-type :focus) anim-vec)
          (conj [object-name (first anim)] [object-name (second anim)]))))
    acc
    [:position :rotation :visibility :alpha :focus]))

(defn- notify-ui [index slides-count]
  (dispatch [::events/update-slide-info index slides-count]))

(defn get-slides []
  (let [slides [{:data {"immersa-text" {:type :text
                                        :text "IMMERSA"
                                        :font-size 72
                                        :font-family "Bellefair,serif"
                                        :line-spacing "10px"
                                        :alpha 0
                                        :color "white"
                                        :text-horizontal-alignment api.const/gui-horizontal-align-center
                                        :text-vertical-alignment api.const/gui-vertical-align-center}}}
                {:data {"immersa-text" {:type :text
                                        :alpha 1}
                        "immersa-text-2" {:type :text
                                          :text "A 3D Presentation Tool for the Web"
                                          :font-size 72
                                          :font-family "Bellefair,serif"
                                          :line-spacing "10px"
                                          :alpha 0
                                          :color "white"
                                          :text-horizontal-alignment api.const/gui-horizontal-align-center
                                          :text-vertical-alignment api.const/gui-vertical-align-center
                                          :padding-top "70%"}}}
                {:data {:camera {:focus "world-earth-sphere"
                                 :type :center}
                        "immersa-text" {:type :text
                                        :alpha 0}
                        "immersa-text-2" {:type :text
                                          :alpha 1}}}
                {:data {"box" {:type :box
                               :position (v3 2 0 0)
                               :rotation (v3 0 2.4 0)
                               :visibility 0.5}
                        "immersa-text-2" {:type :text
                                          :alpha 0}}}

                {:data {"box" {:type :box
                               :position (v3 0 2 0)
                               :rotation (v3 1.2 2.3 4.1)
                               :visibility 1}}}

                {:data {:camera {:focus "box"
                                 :type :left}
                        "billboard-1" {:type :billboard
                                       :position (v3 3 2.3 0)
                                       :text "\n❖ 3D Immersive Experience\n\n❖ Web-Based Accessibility\n\n❖ AI-Powered Features\n\n"
                                       :scale 2
                                       :font-weight "bold"
                                       :visibility 1}
                        "box" {}}}

                {:data {:camera {:focus "box"
                                 :type :right}
                        "billboard-2" {:type :billboard
                                       :position (v3 -3 2.3 0)
                                       :text "❖ Ready-Made Templates\n\n❖ High-Performance Render\n\n❖ User-Friendly UI/UX"
                                       :scale 2
                                       :font-weight "bold"}
                        "box" {}}}

                {:data {:camera {:position (v3 0 0 -10)
                                 :rotation (v3 0 0 0)}
                        "box" {}}}

                {:data {"immersa-text-3" {:type :text
                                          :text "✦Enjoy the Immersive Experience✦"
                                          :font-size 72
                                          :font-family "Bellefair,serif"
                                          :line-spacing "10px"
                                          :alpha 1
                                          :color "white"
                                          :text-horizontal-alignment api.const/gui-horizontal-align-center
                                          :text-vertical-alignment api.const/gui-vertical-align-center}}}

                {:data {:camera {:position (v3 0 0 50)}
                        "immersa-text-3" {:alpha 0}}}]
        slides-vec (vec (map-indexed #(assoc %2 :index %1) slides))
        props-to-copy [:type :position :rotation :visibility]
        clone-if-exists (fn [data]
                          (cond-> data
                            (:position data) (assoc :position (api.core/clone (:position data)))
                            (:rotation data) (assoc :rotation (api.core/clone (:rotation data)))))]
    (reduce
      (fn [slides-vec slide]
        (let [prev-slide-data (get-in slides-vec [(dec (:index slide)) :data])
              slide-data (:data slide)]
          (conj slides-vec
                (assoc slide :data
                       (reduce-kv
                         (fn [acc name objet-slide-data]
                           (if-let [prev-slide-data (get prev-slide-data name)]
                             (assoc acc name (merge (clone-if-exists (select-keys prev-slide-data props-to-copy)) objet-slide-data))
                             (assoc acc name objet-slide-data)))
                         {}
                         slide-data)))))
      [(first slides-vec)]
      (rest slides-vec))))

(comment
  (let [command-ch (a/chan (a/dropping-buffer 1))]
    (a/put! command-ch :next)
    (api.core/dispose-all (concat (api.core/get-objects-by-type "box")
                                  (api.core/get-objects-by-type "billboard")
                                  (api.core/get-objects-by-type "text")))
    (api.camera/reset-camera)
    (api.camera/detach-control (api.camera/active-camera))
    (common.utils/remove-element-listeners)

    (common.utils/register-event-listener js/window "keydown"
                                          (fn [e]
                                            (when-not (j/get e :repeat)
                                              (cond
                                                (or (= (.-keyCode e) 39)
                                                    (= (.-keyCode e) 40)) (a/put! command-ch :next)
                                                (or (= (.-keyCode e) 37)
                                                    (= (.-keyCode e) 38)) (a/put! command-ch :prev)))))
    (go-loop [index -1]
             (let [command (a/<! command-ch)
                   next-index (case command
                                :next (inc index)
                                :prev (dec index))
                   slides (get-slides)]
               (if (and (>= next-index 0) (< next-index (count slides)))
                 (let [_ (notify-ui next-index (count slides))
                       slide (slides next-index)
                       objects-data (:data slide)
                       object-names-from-slide-info (set (conj (keys (:data slide)) :camera))
                       _ (when (object-names-from-slide-info :camera)
                           (api.camera/update-active-camera))
                       object-names-from-objects (-> slide :objects keys)
                       objects-to-create (filter #(not (api.core/get-object-by-name %)) object-names-from-slide-info)
                       object-names-to-dispose (when (> next-index 0)
                                                 (let [prev-slide (slides (if (= command :next)
                                                                            (dec next-index)
                                                                            (inc next-index)))
                                                       prev-slide-object-names (-> prev-slide :data keys set)
                                                       current-slide-object-names (-> slide :data keys set)]
                                                   (set/difference prev-slide-object-names current-slide-object-names #{:camera})))

                       #_#__ (doseq [name object-names-to-dispose]
                               (api.core/dispose name))
                       _ (doseq [name objects-to-create]
                           (let [params (get objects-data name)
                                 type (:type params)
                                 params (dissoc params :type)]
                             (case type
                               :box (api.component/create-box-with-numbers name params)
                               :text3D (api.mesh/text name params)
                               :text (api.gui/add-control
                                       (api.core/get-advanced-texture)
                                       (api.gui/text-block name params))
                               :billboard (api.component/billboard name params)
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
                                             (let [animations (mapv second animations)
                                                   max-fps (apply max (map (j/get :framePerSecond) animations))]
                                               (assoc acc name {:target (api.core/get-object-by-name name)
                                                                :animations animations
                                                                :from 0
                                                                :to max-fps})))
                                           {}
                                           (group-by first animations)))
                       channels (mapv #(api.animation/begin-direct-animation %) animations-data)]
                   (doseq [c channels]
                     (a/<! c))
                   (recur next-index))
                 (recur index))))))

(defn register-before-render []
  (let [delta (api.core/get-delta-time)]
    (some-> (api.core/get-object-by-name "sky-box") (j/update-in! [:rotation :y] #(+ % (* 0.008 delta))))
    (some-> (api.core/get-object-by-name "world-earth-node") (j/update-in! [:rotation :y] #(- % (* 0.05 delta))))
    (some-> (api.core/get-object-by-name "earth2") (j/update-in! [:rotation :y] #(- % (* 0.05 delta))))
    (some-> (api.core/get-object-by-name "cloud") (j/update-in! [:rotation :y] #(- % (* 0.07 delta))))))

(defn when-scene-ready [scene]
  (api.core/clear-scene-color api.const/color-white)
  (j/assoc-in! (api.core/get-object-by-name "sky-box") [:rotation :y] js/Math.PI)
  (api.gui/advanced-dynamic-texture)
  (j/call scene :registerBeforeRender (fn [] (register-before-render))))

(defn start-scene [canvas]
  (let [engine (api.core/create-engine canvas)
        scene (api.core/create-scene engine)
        camera (api.camera/create-free-camera "free-camera" :position (v3 0 0 -10))
        light (api.light/hemispheric-light "light")
        light2 (api.light/directional-light "light2"
                                            :position (v3 20)
                                            :dir (v3 -1 -2 0))
        ground-material (api.material/grid-mat "grid-mat"
                                               :major-unit-frequency 5
                                               :minor-unit-visibility 0.45
                                               :grid-ratio 2
                                               :back-face-culling? false
                                               :main-color (api.core/color 1 1 1)
                                               :line-color (api.core/color 1 1 1)
                                               :opacity 0.98)
        ground (api.mesh/create-ground "ground"
                                       :width 50
                                       :height 50
                                       :mat ground-material)
        sky-box (api.component/create-sky-box)]
    (j/assoc! light :intensity 0.7)
    (j/call camera :setTarget (v3))
    (j/call camera :attachControl canvas false)
    (j/call engine :runRenderLoop #(j/call scene :render))
    (j/call scene :executeWhenReady #(when-scene-ready scene))))

(defn restart-engine []
  (api.core/dispose-engine)
  (start-scene (js/document.getElementById "renderCanvas")))

(comment
  (api.camera/reset-camera)
  (api.component/earth :name "world"
                       :position (v3 0 -0.7 -8.5))
  (restart-engine))
