(ns immersa.scene.core
  (:require
    ["@babylonjs/core/Meshes/Compression/dracoCompression" :refer [DracoCompression]]
    ["@babylonjs/core/Rendering/boundingBoxRenderer"]
    [applied-science.js-interop :as j]
    [cljs.core.async :as a]
    [cljs.core.async :as a :refer [go-loop <!]]
    [cljs.reader :as reader]
    [clojure.string :as str]
    [com.rpl.specter :as sp]
    [goog.functions :as functions]
    [immersa.common.firebase :as firebase]
    [immersa.common.utils :as common.utils]
    [immersa.scene.api.animation :as api.anim]
    [immersa.scene.api.camera :as api.camera]
    [immersa.scene.api.component :as api.component]
    [immersa.scene.api.constant :as api.const]
    [immersa.scene.api.core :as api.core :refer [v3]]
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
                 (let [click-type (case (j/get-in info [:event :button])
                                    0 :left-click?
                                    1 :middle-click?
                                    2 :right-click?
                                    nil)]
                   (when (and (= api.const/pointer-type-tap (j/get info :type))
                              (not (j/get-in info [:pickInfo :hit])))
                     (api.core/clear-selected-mesh))
                   (cond
                     (and click-type (= (j/get info :type) api.const/pointer-type-down))
                     (j/assoc-in! api.core/db [:mouse click-type] true)

                     (and click-type (= (j/get info :type) api.const/pointer-type-up))
                     (j/assoc-in! api.core/db [:mouse click-type] false))
                   (api.camera/switch-camera-if-needed scene))))
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

                     (and (= key "escape")
                          (= (j/get info :type) api.const/keyboard-type-key-down))
                     (api.core/clear-selected-mesh)

                     (and (= key "f")
                          (= (j/get info :type) api.const/keyboard-type-key-down)
                          (j/get-in api.core/db [:gizmo :selected-mesh]))
                     (api.anim/run-camera-focus-anim (j/get-in api.core/db [:gizmo :selected-mesh]))
                     #_(j/call (api.camera/active-camera)
                               :setTarget (api.core/clone (j/get-in api.core/db [:gizmo :selected-mesh :position])))

                     (and (= key "c")
                          (= (j/get info :type) api.const/keyboard-type-key-down)
                          (j/get-in info [:event :metaKey])
                          (api.core/selected-mesh))
                     ;; TODO  move this to data structure so browser does not have to ask for permission
                     (-> (api.core/selected-mesh)
                         api.core/get-object-name
                         (#(vector % (get-in @slide/all-slides [@slide/current-slide-index :data %])))
                         common.utils/copy-to-clipboard)

                     (and (= key "v")
                          (= (j/get info :type) api.const/keyboard-type-key-up)
                          (j/get-in info [:event :metaKey]))
                     (-> (j/call-in js/navigator [:clipboard :readText])
                         (j/call :then (fn [text]
                                         (when-not (str/blank? text)
                                           (try
                                             (slide/duplicate-slide-data (reader/read-string text))
                                             (catch js/Error e
                                               (js/console.warn "Clipboard data is not in EDN format.")
                                               (js/console.warn e))))))
                         (j/call :catch (fn []
                                          (js/console.error "Clipboard failed."))))
                     (and (= (j/get info :type) api.const/keyboard-type-key-down)
                          (= key "backspace")
                          (api.core/selected-mesh))
                     (let [obj (api.core/selected-mesh)
                           id (api.core/get-object-name obj)
                           current-index @slide/current-slide-index]
                       (api.core/clear-selected-mesh)
                       (sp/setval [sp/ATOM current-index :data id] sp/NONE slide/all-slides)
                       (sp/setval [sp/ATOM id] sp/NONE slide/prev-slide)
                       (api.core/set-enabled obj false))

                     (and (= (j/get info :type) api.const/keyboard-type-key-down)
                          (= key "t"))
                     (ui-listener/handle-ui-update {:type :add-text-mesh})

                     (and (= (j/get info :type) api.const/keyboard-type-key-down)
                          (= key "n"))
                     (ui-listener/handle-ui-update {:type :add-slide})

                     (and (= (j/get info :type) api.const/keyboard-type-key-down)
                          (= key "1")
                          (api.core/selected-mesh))
                     (api.gizmo/toggle-gizmo :position)

                     (and (= (j/get info :type) api.const/keyboard-type-key-down)
                          (= key "2")
                          (api.core/selected-mesh))
                     (api.gizmo/toggle-gizmo :rotation)

                     (and (= (j/get info :type) api.const/keyboard-type-key-down)
                          (= key "3")
                          (api.core/selected-mesh))
                     (api.gizmo/toggle-gizmo :scale))
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
  (let [arc-camera (api.core/get-object-by-name "arc-camera")
        free-camera (api.core/get-object-by-name "free-camera")
        f #(when-not (j/get api.core/db :lock-view-matrix-change?)
             (dispatch [::editor.events/set-camera (utils/v3->v-data free-camera [:position :rotation])]))
        update-camera-on-slide (functions/debounce
                                 (fn []
                                   (when (= (api.camera/active-camera) arc-camera)
                                     (let [position (api.core/clone (j/get arc-camera :position))
                                           target (j/call arc-camera :getTarget)]
                                       (j/call-in free-camera [:position :copyFrom] position)
                                       (j/call free-camera :setTarget (api.core/clone target))))
                                   (let [position (api.core/v3->v (j/get free-camera :position))
                                         rotation (api.core/v3->v (j/get free-camera :rotation))]
                                     (slide/update-slide-data :camera :position position)
                                     (slide/update-slide-data :camera :rotation rotation)))
                                 200)]
    (j/call-in free-camera [:onViewMatrixChangedObservable :add] f)
    (j/call-in free-camera [:onViewMatrixChangedObservable :add] update-camera-on-slide)
    (j/call-in arc-camera [:onViewMatrixChangedObservable :add] update-camera-on-slide)
    (dispatch [::editor.events/set-camera (utils/v3->v-data free-camera [:position :rotation])])))

(defn- update-bounding-box-renderer [scene]
  (j/assoc! (j/call scene :getBoundingBoxRenderer) :showBackLines false))

(defn when-scene-ready [engine scene mode slides thumbnails]
  (api.core/clear-scene-color (api.const/color-white))
  (j/assoc-in! (api.core/get-object-by-name "sky-box") [:rotation :y] js/Math.PI)
  (api.gui/advanced-dynamic-texture)
  (j/call scene :registerBeforeRender (fn [] (register-before-render)))
  (case mode
    :editor (do
              (api.gizmo/init-gizmo-manager)
              (register-scene-mouse-events scene)
              (ui-listener/init-ui-update-listener)
              (add-camera-view-matrix-listener)
              (update-bounding-box-renderer scene)
              (slide/start-slide-show {:mode mode
                                       :slides slides
                                       :thumbnails thumbnails}))
    :present (do
               (slide/start-slide-show {:mode mode
                                        :slides slides})
               (start-background-lighting engine))))

(defn- update-draco-url []
  (j/assoc-in! DracoCompression [:Configuration :decoder]
               #js {:wasmUrl "js/draco/draco_wasm_wrapper_gltf.js"
                    :wasmBinaryUrl "js/draco/draco_decoder_gltf.wasm"
                    :wasmBinaryFile "js/draco/draco_decoder_gltf.js"}))

(defn start-scene [canvas & {:keys [start-slide-show?
                                    mode
                                    dev?
                                    slides
                                    thumbnails]
                             :or {start-slide-show? true}}]
  (a/go
    (let [engine (api.core/create-engine canvas)
          scene (api.core/create-scene engine)
          _ (update-draco-url)
          _ (api.core/create-assets-manager :on-finish #(do
                                                          (dispatch [::events/set-show-arrow-keys-text? false])
                                                          (dispatch [::events/set-show-pre-warm-text? true])))
          _ (firebase/init-app)
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
          _ (api.component/create-sky-box)
          ;; _ (api.component/create-sky-sphere)
          _ (api.material/create-environment-helper)
          _ (a/<! (api.core/load-async slides))
          ;; _ (api.material/init-nme-materials)
          ]
      (when dev?
        (common.utils/remove-element-listeners))
      (common.utils/register-event-listener js/window "resize" (functions/debounce #(j/call engine :resize) 250))
      (j/assoc! light :intensity 0.7)
      (j/call free-camera :setTarget (v3))
      (j/call free-camera :attachControl canvas false)
      (j/call engine :runRenderLoop #(j/call scene :render))
      (j/call scene :executeWhenReady #(when-scene-ready engine scene mode slides thumbnails)))))

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

  (restart-engine :start-slide-show? true)
  (restart-engine :start-slide-show? false)
  )
