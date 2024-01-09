(ns immersa.scene.core
  (:require
    [applied-science.js-interop :as j]
    [cljs.core.async :as a]
    [cljs.core.async :as a :refer [go-loop <!]]
    [clojure.string :as str]
    [goog.functions :as functions]
    [immersa.common.utils :as common.utils]
    [immersa.events :as events]
    [immersa.scene.api.camera :as api.camera]
    [immersa.scene.api.component :as api.component]
    [immersa.scene.api.constant :as api.const]
    [immersa.scene.api.core :as api.core :refer [v2 v3 v4]]
    [immersa.scene.api.gui :as api.gui]
    [immersa.scene.api.light :as api.light]
    [immersa.scene.api.material :as api.material]
    [immersa.scene.api.mesh :as api.mesh]
    [immersa.scene.slide :as slide]
    [re-frame.core :refer [dispatch]]))

(defn register-before-render []
  (let [delta (api.core/get-delta-time)]
    (j/update! api.core/db :elapsed-time (fnil + 0) delta)
    (some-> (api.core/get-object-by-name "sky-box") (j/update-in! [:rotation :y] #(+ % (* 0.008 delta))))
    (some-> (api.core/get-object-by-name "world") (j/update-in! [:rotation :y] #(- % (* 0.05 delta))))
    (doseq [f (api.core/get-before-render-fns)]
      (f))))

(defn switch-camera-if-needed [scene]
  (let [wasd? (boolean (seq (filter true? (map #(j/get-in api.core/db [:keyboard %]) ["w" "a" "s" "d"]))))
        left-click? (j/get-in api.core/db [:mouse :left-click?])
        switch-type (cond
                      wasd? :free
                      (and left-click? (not wasd?)) :arc
                      :else :arc)
        camera (api.camera/active-camera)
        camera-type (j/get camera :type)
        arc-camera (api.core/get-object-by-name "arc-camera")
        free-camera (api.core/get-object-by-name "free-camera")
        canvas (api.core/canvas)]
    (cond
      (and (= switch-type :free) (not (= camera-type :free)))
      (let [position (api.core/clone (j/get arc-camera :position))
            target (j/call arc-camera :getTarget)]
        (j/call-in free-camera [:position :copyFrom] position)
        (j/call free-camera :setTarget (api.core/clone target))
        (j/assoc! scene :activeCamera free-camera)
        (api.camera/detach-control arc-camera)
        (j/call free-camera :attachControl canvas true))

      (and (= switch-type :arc) (not (= camera-type :arc)))
      (let [position (api.core/clone (j/get free-camera :position))
            target (j/call free-camera :getTarget)]
        (api.core/set-pos arc-camera position)
        (j/call arc-camera :setTarget (api.core/clone target))
        (j/assoc! scene :activeCamera arc-camera)
        (api.camera/detach-control free-camera)
        (j/call arc-camera :attachControl canvas true)))))

(defn- register-scene-mouse-events [scene]
  (let [wasd #{"w" "a" "s" "d"}]
    (j/call-in scene [:onPointerObservable :add]
               (fn [info]
                 (cond
                   (= (j/get info :type) api.const/pointer-type-down)
                   (j/assoc-in! api.core/db [:mouse :left-click?] true)

                   (= (j/get info :type) api.const/pointer-type-up)
                   (j/assoc-in! api.core/db [:mouse :left-click?] false))
                 (switch-camera-if-needed scene)))
    (j/call-in scene [:onKeyboardObservable :add]
               (fn [info]
                 (let [key (j/get-in info [:event :key])]
                   (cond
                     (and (= (j/get info :type) api.const/keyboard-type-key-down)
                          (wasd (j/get-in info [:event :key])))
                     (j/assoc-in! api.core/db [:keyboard key] true)

                     (and (= (j/get info :type) api.const/keyboard-type-key-up)
                          (wasd (j/get-in info [:event :key])))
                     (j/assoc-in! api.core/db [:keyboard key] false))
                   (switch-camera-if-needed scene))))))

(defn- read-pixels [engine]
  (let [p (a/promise-chan)
        canvas (j/call engine :getRenderingCanvas)
        width (j/get canvas :width)
        height (j/get canvas :height)
        pixels (j/call engine :readPixels 0 0 width height)]
    (j/call pixels :then #(a/put! p {:pixels %
                                     :width width
                                     :height height}))
    p))

(defn- lerp-colors [old-color new-color]
  (let [p (a/promise-chan)
        elapsed-time (atom 0)
        transition-duration 0.5
        fn-name "lerp-bg-colors"
        _ (api.core/register-before-render-fn
            fn-name
            (fn []
              (let [elapsed-time (swap! elapsed-time + (api.core/get-delta-time))
                    amount (min (/ elapsed-time transition-duration) 1)
                    color (api.core/color-lerp old-color new-color amount)
                    color-str (str "rgb(" (j/get color :r) "," (j/get color :g) "," (j/get color :b) ")")]
                (dispatch [::events/set-background-color color-str])
                (when (>= elapsed-time transition-duration)
                  (api.core/remove-before-render-fn fn-name)
                  (a/put! p true)))))]
    p))

(defn- strong-machine? [engine]
  (let [gl (j/call engine :getGlInfo)
        renderer (j/get gl :renderer)]
    (boolean
      (when-not (str/blank? renderer)
        (some #(str/includes? renderer %) ["Apple M1" "Apple M2" "Apple M3"])))))

;; TODO check OffscreenCanvas support
(defn- start-background-lighting [engine]
  (when (and (j/get js/window :Worker) (strong-machine? engine))
    (let [worker (js/Worker. "js/worker/worker.js")
          color-ch (a/chan (a/dropping-buffer 1))
          prev-color (atom (api.core/color 0 0 0))]
      (a/put! color-ch #js[0 0 0])
      (j/assoc! worker :onmessage #(a/put! color-ch (j/get % :data)))
      (go-loop []
        (let [{:keys [pixels width height]} (<! (read-pixels engine))
              color (<! color-ch)
              new-color (api.core/color (j/get color 0) (j/get color 1) (j/get color 2))
              _ (when-not (api.core/equals? @prev-color new-color)
                  (<! (lerp-colors @prev-color new-color)))]
          (reset! prev-color new-color)
          (j/call worker :postMessage #js[pixels width height])
          (<! (a/timeout 500))
          (recur))))))

(defn when-scene-ready [engine scene start-slide-show?]
  (api.core/clear-scene-color api.const/color-white)
  ;; (api.core/clear-scene-color (api.core/color-rgb 239 239 239))
  ;; (api.core/clear-scene-color api.const/color-black)
  (j/assoc-in! (api.core/get-object-by-name "sky-box") [:rotation :y] js/Math.PI)
  (api.gui/advanced-dynamic-texture)
  (j/call scene :registerBeforeRender (fn [] (register-before-render)))
  (when-not start-slide-show?
    (register-scene-mouse-events scene))
  (when start-slide-show?
    (slide/start-slide-show)
    (start-background-lighting engine)))

(defn start-scene [canvas & {:keys [start-slide-show?]
                             :or {start-slide-show? true}}]
  (a/go
    (let [engine (api.core/create-engine canvas)
          scene (api.core/create-scene engine)
          _ (api.core/create-assets-manager :on-finish #(dispatch [::events/set-show-arrow-keys-text? false]))
          _ (a/<! (api.core/load-async))
          _ (api.core/init-p5)
          free-camera (api.camera/create-free-camera "free-camera" :position (v3 0 0 -10))
          arc-camera (api.camera/create-arc-camera "arc-camera"
                                                   :position (v3 0 0 -10)
                                                   :canvas canvas)
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
          _ (api.component/create-sky-box)
          _ (api.component/create-sky-sphere)
          _ (api.material/create-environment-helper)]
      (common.utils/remove-element-listeners)
      (common.utils/register-event-listener js/window "resize" (functions/debounce #(j/call engine :resize) 250))
      (j/assoc! light :intensity 0.7)
      (j/call free-camera :setTarget (v3))
      (j/call free-camera :attachControl canvas false)
      (j/call engine :runRenderLoop #(j/call scene :render))
      (j/call scene :executeWhenReady #(when-scene-ready engine scene start-slide-show?)))))

(defn restart-engine [& {:keys [start-slide-show?]
                         :or {start-slide-show? true}}]
  (api.core/dispose-engine)
  (start-scene (js/document.getElementById "renderCanvas") :start-slide-show? start-slide-show?))

(comment
  (common.utils/register-event-listener js/window "resize" #(do
                                                              (println "resized!")
                                                              ))

  (j/call (api.camera/active-camera) :attachControl (api.core/canvas) true)

  (api.core/show-debug)
  (api.camera/reset-camera)

  (restart-engine)

  (restart-engine :start-slide-show? false))
