(ns immersa.scene.core
  (:require
    [applied-science.js-interop :as j]
    [cljs.core.async :as a]
    [immersa.common.utils :as common.utils]
    [immersa.scene.api.camera :as api.camera]
    [immersa.scene.api.component :as api.component]
    [immersa.scene.api.constant :as api.const]
    [immersa.scene.api.core :as api.core :refer [v2 v3 v4]]
    [immersa.scene.api.gui :as api.gui]
    [immersa.scene.api.light :as api.light]
    [immersa.scene.api.material :as api.material]
    [immersa.scene.api.mesh :as api.mesh]
    [immersa.scene.slide :as slide])
  (:require-macros
    [shadow.resource :as rc]))

(defn register-before-render []
  (let [delta (api.core/get-delta-time)]
    (some-> (api.core/get-object-by-name "sky-box") (j/update-in! [:rotation :y] #(+ % (* 0.008 delta))))
    (some-> (api.core/get-object-by-name "world") (j/update-in! [:rotation :y] #(- % (* 0.05 delta))))
    (doseq [f (api.core/get-before-render-fns)]
      (f))))

(defn when-scene-ready [scene start-slide-show?]
  ;; (api.core/clear-scene-color api.const/color-white)
  ;; (api.core/clear-scene-color (api.core/color-rgb 239 239 239))
  (api.core/clear-scene-color api.const/color-black)
  (j/assoc-in! (api.core/get-object-by-name "sky-box") [:rotation :y] js/Math.PI)
  (api.gui/advanced-dynamic-texture)
  (j/call scene :registerBeforeRender (fn [] (register-before-render)))
  (when start-slide-show?
    (slide/start-slide-show)))

(defn start-scene [canvas & {:keys [start-slide-show?]
                             :or {start-slide-show? true}}]
  (a/go
    (let [engine (api.core/create-engine canvas)
          scene (api.core/create-scene engine)
          _ (api.core/create-assets-manager)
          _ (a/<! (api.core/load-async))
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
      ;; TODO add debounce here
      (common.utils/register-event-listener js/window "resize" #(j/call engine :resize))
      (j/assoc! light :intensity 0.7)
      (j/call camera :setTarget (v3))
      (j/call camera :attachControl canvas false)
      (j/call engine :runRenderLoop #(j/call scene :render))
      (j/call scene :executeWhenReady #(when-scene-ready scene start-slide-show?)))))

(defn restart-engine [& {:keys [start-slide-show?]
                         :or {start-slide-show? true}}]
  (api.core/dispose-engine)
  (start-scene (js/document.getElementById "renderCanvas") :start-slide-show? start-slide-show?))

(comment

  (j/call (api.camera/active-camera) :attachControl (api.core/canvas) true)

  (api.core/show-debug)
  (api.camera/reset-camera)
  (restart-engine :start-slide-show? false))
