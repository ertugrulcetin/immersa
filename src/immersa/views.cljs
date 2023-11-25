(ns immersa.views
  (:require
    ["@babylonjs/core/Cameras/freeCamera" :refer [FreeCamera]]
    ["@babylonjs/core/Engines/engine" :refer [Engine]]
    ["@babylonjs/core/Lights/hemisphericLight" :refer [HemisphericLight]]
    ["@babylonjs/core/Materials/standardMaterial" :refer [StandardMaterial]]
    ["@babylonjs/core/Maths/math" :refer [Vector3]]
    ["@babylonjs/core/Meshes/meshBuilder" :refer [MeshBuilder]]
    ["@babylonjs/core/scene" :refer [Scene]]
    [applied-science.js-interop :as j]
    [immersa.scene.api :as api]
    [immersa.styles :as styles]
    [immersa.subs :as subs]
    [re-frame.core :as re-frame]))

(defn main-panel []
  [:div (styles/app-container)
   [:div (styles/toolbar)]
   [:div (styles/content)
    [:div (styles/sidebar)]
    [:div (styles/canvas-container)
     [:div (styles/canvas-wrapper)
      [:canvas
       {:id "renderCanvas"
        :ref (fn [canvas]
               (js/console.log "Initializing canvas")
               (let [engine (api/create-engine canvas)
                     scene (Scene. engine)
                     camera (FreeCamera. "camera1" (Vector3. 0 5 -10))
                     light (HemisphericLight. "light1" (Vector3. 0 1 0))
                     box (j/call MeshBuilder :CreateBox "my-box" #js {:size 1})
                     ground (j/call MeshBuilder :CreateGround "ground1" #js {:width 6
                                                                             :height 6
                                                                             :subdivisions 2
                                                                             :updatable false})]
                 (js/console.log ground)
                 (j/assoc! light :intensity 0.7)
                 (.setTarget camera (Vector3. 0 0 0))
                 (.attachControl camera canvas false)
                 (.runRenderLoop engine (fn [] (.render scene)))))
        :class (styles/canvas)}]]
     [:div (styles/canvas-footer)
      [:p "selam"]]]
    [:div (styles/options-bar)]]])
