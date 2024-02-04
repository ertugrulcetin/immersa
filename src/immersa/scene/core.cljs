(ns immersa.scene.core
  (:require
    ["@babylonjs/core/Meshes/Compression/dracoCompression" :refer [DracoCompression]]
    [applied-science.js-interop :as j]
    [cljs.core.async :as a]
    [cljs.core.async :as a :refer [go-loop <!]]
    [clojure.string :as str]
    [goog.functions :as functions]
    [immersa.common.utils :as common.utils]
    [immersa.scene.api.animation :as api.anim]
    [immersa.scene.api.camera :as api.camera]
    [immersa.scene.api.constant :as api.const]
    [immersa.scene.api.core :as api.core :refer [v2 v3 v4]]
    [immersa.scene.api.gizmo :as api.gizmo]
    [immersa.scene.api.gui :as api.gui]
    [immersa.scene.api.light :as api.light]
    [immersa.scene.api.material :as api.material]
    [immersa.scene.api.mesh :as api.mesh]
    [immersa.scene.slide :as slide]
    [immersa.scene.ui-listener :as ui-listener]
    [immersa.scene.utils :as utils]
    [immersa.ui.editor.events :as editor.events]
    [immersa.ui.present.events :as events]
    [re-frame.core :refer [dispatch]]))

(defn register-before-render []
  (let [delta (api.core/get-delta-time)]
    (j/update! api.core/db :elapsed-time (fnil + 0) delta)
    (some-> (api.core/get-object-by-name "sky-box") (j/update-in! [:rotation :y] #(+ % (* 0.008 delta))))
    (some-> (api.core/get-object-by-name "world") (j/update-in! [:rotation :y] #(- % (* 0.05 delta))))
    (doseq [f (api.core/get-before-render-fns)]
      (f))))

(defn- register-scene-mouse-events [scene]
  (let [wasd #{"w" "a" "s" "d" "e" "q"}]
    (j/call-in scene [:onPointerObservable :add]
               (fn [info]
                 (cond
                   (= (j/get info :type) api.const/pointer-type-down)
                   (j/assoc-in! api.core/db [:mouse :left-click?] true)

                   (= (j/get info :type) api.const/pointer-type-up)
                   (j/assoc-in! api.core/db [:mouse :left-click?] false))
                 (api.camera/switch-camera-if-needed scene)))
    (j/call-in scene [:onKeyboardObservable :add]
               (fn [info]
                 (let [key (str/lower-case (j/get-in info [:event :key]))]
                   (cond
                     (and (= (j/get info :type) api.const/keyboard-type-key-down)
                          (wasd key))
                     (j/assoc-in! api.core/db [:keyboard key] true)

                     (and (= (j/get info :type) api.const/keyboard-type-key-up)
                          (wasd key))
                     (j/assoc-in! api.core/db [:keyboard key] false)

                     (= key "escape")
                     (api.gizmo/clear-selected-mesh)

                     (and (= key "f") (j/get-in api.core/db [:gizmo :selected-mesh]))
                     (api.anim/run-camera-focus-anim (j/get-in api.core/db [:gizmo :selected-mesh]))
                     #_(j/call (api.camera/active-camera)
                               :setTarget (api.core/clone (j/get-in api.core/db [:gizmo :selected-mesh :position]))))
                   (api.camera/switch-camera-if-needed scene))))))

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

(defn- strong-machine? [engine]
  (let [gl (j/call engine :getGlInfo)
        renderer (j/get gl :renderer)]
    (boolean
      (when-not (str/blank? renderer)
        (some #(str/includes? renderer %) ["Apple M1" "Apple M2" "Apple M3"])))))

(defn- start-background-lighting [engine]
  (when (and (j/get js/window :Worker) (strong-machine? engine))
    (let [worker (js/Worker. "js/worker/worker.js")
          color-ch (a/chan (a/dropping-buffer 1))
          prev-color (atom (api.core/color 0 0 0))
          on-lerp (fn [r g b]
                    (let [color-str (str "rgb(" r "," g "," b ")")]
                      (dispatch [::events/set-background-color color-str])))]
      (a/put! color-ch #js[0 0 0])
      (j/assoc! worker :onmessage #(a/put! color-ch (j/get % :data)))
      (go-loop []
        (let [{:keys [pixels width height]} (<! (read-pixels engine))
              color (<! color-ch)
              new-color (api.core/color (j/get color 0) (j/get color 1) (j/get color 2))
              _ (when-not (api.core/equals? @prev-color new-color)
                  (<! (:ch (api.core/lerp-colors {:start-color @prev-color
                                                  :end-color new-color
                                                  :on-lerp on-lerp}))))]
          (reset! prev-color new-color)
          (j/call worker :postMessage #js[pixels width height])
          (<! (a/timeout 500))
          (recur))))))

(defn- add-camera-view-matrix-listener []
  (let [free-camera (api.core/get-object-by-name "free-camera")
        f #(when-not (j/get api.core/db :lock-view-matrix-change?)
             (dispatch [::editor.events/set-camera (utils/v3->v-data free-camera [:position :rotation])]))]
    (j/call-in free-camera [:onViewMatrixChangedObservable :add] f)
    (dispatch [::editor.events/set-camera (utils/v3->v-data free-camera [:position :rotation])])))

(defn when-scene-ready [engine scene mode]
  (api.core/clear-scene-color (api.const/color-white))
  (j/assoc-in! (api.core/get-object-by-name "sky-box") [:rotation :y] js/Math.PI)
  (api.gui/advanced-dynamic-texture)
  (j/call scene :registerBeforeRender (fn [] (register-before-render)))
  (case mode
    :editor (do
              (api.core/hide-loading-ui)
              (register-scene-mouse-events scene)
              (ui-listener/init-ui-update-listener)
              (add-camera-view-matrix-listener))
    :present (do
               (slide/start-slide-show)
               (start-background-lighting engine))))

(defn- update-draco-url []
  (j/assoc-in! DracoCompression [:Configuration :decoder]
               #js {:wasmUrl "js/draco/draco_wasm_wrapper_gltf.js"
                    :wasmBinaryUrl "js/draco/draco_decoder_gltf.wasm"
                    :wasmBinaryFile "js/draco/draco_decoder_gltf.js"}))

(defn start-scene [canvas & {:keys [start-slide-show?
                                    mode
                                    dev?]
                             :or {start-slide-show? true}}]
  (a/go
    (let [engine (api.core/create-engine canvas)
          scene (api.core/create-scene engine)
          _ (update-draco-url)
          _ (api.core/create-assets-manager :on-finish #(do
                                                          (dispatch [::events/set-show-arrow-keys-text? false])
                                                          (dispatch [::events/set-show-pre-warm-text? true])))
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
                                         :mat ground-material
                                         :pickable? false)
          ;; _ (api.component/create-sky-box)
          ;; _ (api.component/create-sky-sphere)
          _ (api.material/create-environment-helper)
          _ (api.material/init-nme-materials)
          ;; TODO only in editor mode, update here
          _ (api.gizmo/init-gizmo-manager)]
      ;; (api.mesh/box "box1")
      (api.mesh/text "test" {:text "Text"
                             :depth 0.001
                             ;; :emissive-color :color/white
                             ;; :size 1
                             })
      (when dev?
        (common.utils/remove-element-listeners))
      (common.utils/register-event-listener js/window "resize" (functions/debounce #(j/call engine :resize) 250))
      (j/assoc! light :intensity 0.7)
      (j/call free-camera :setTarget (v3))
      (j/call free-camera :attachControl canvas false)
      (j/call engine :runRenderLoop #(j/call scene :render))
      (j/call scene :executeWhenReady #(when-scene-ready engine scene mode)))))

(defn restart-engine [& {:keys [start-slide-show?
                                dev?]
                         :or {start-slide-show? true}}]
  (api.core/dispose-engine)
  (start-scene (js/document.getElementById "renderCanvas")
               :mode :editor
               :dev? dev?))

(comment
  (common.utils/register-event-listener js/window "resize" #(do
                                                              (println "resized!")
                                                              ))

  (j/call (api.camera/active-camera) :attachControl (api.core/canvas) true)

  (api.core/show-debug)

  (api.camera/reset-camera)

  (restart-engine)

  (restart-engine :start-slide-show? false))
