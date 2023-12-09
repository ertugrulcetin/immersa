(ns immersa.views
  (:require
    ["@babylonjs/core/Cameras/freeCamera" :refer [FreeCamera]]
    ["@babylonjs/core/Engines/engine" :refer [Engine]]
    ["@babylonjs/core/Lights/hemisphericLight" :refer [HemisphericLight]]
    ["@babylonjs/core/Materials/standardMaterial" :refer [StandardMaterial]]
    ["@babylonjs/core/Maths/math" :refer [Vector3]]
    ["@babylonjs/core/Meshes/meshBuilder" :refer [MeshBuilder]]
    ["@babylonjs/core/scene" :refer [Scene]]
    ["@phosphor-icons/react" :refer [CaretLeft CaretRight DotsThreeVertical ChatCenteredText SmileySticker Command]]
    ["progressbar.js" :as ProgressBar]
    [immersa.events :as events]
    [immersa.scene.core :as scene.core]
    [immersa.styles :as styles]
    [immersa.subs :as subs]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]))

(def icon-prev (r/adapt-react-class CaretLeft))
(def icon-next (r/adapt-react-class CaretRight))
(def icon-dots (r/adapt-react-class DotsThreeVertical))
(def icon-chat (r/adapt-react-class ChatCenteredText))
(def icon-smiley (r/adapt-react-class SmileySticker))
(def icon-control (r/adapt-react-class Command))

(defn- canvas []
  (r/create-class
    {:component-did-mount #(scene.core/start-scene (js/document.getElementById "renderCanvas"))
     :reagent-render (fn []
                       [:canvas
                        {:id "renderCanvas"
                         :class (styles/canvas)}])}))

(defn- canvas-container []
  ;; TODO we need to notify the engine when the canvas dimensions change
  (let [{:keys [width height] :as dimensions} @(subscribe [::subs/calculated-canvas-dimensions])]
    (when (and (> width 0) (> height 0))
      [:div
       {:id "canvas-container"
        :class (styles/canvas-container dimensions)}
       [canvas]])))

(defn- progress-bar-line []
  [:div
   {:id "progress-line"
    :class (styles/progress-line)
    :ref (fn [ref]
           (when ref
             (let [bar (ProgressBar/Line. ref
                                          #js {:strokeWidth 1
                                               :easing "easeInOut"
                                               :duration 500
                                               :color "white"
                                               :trailColor "rgb(63, 66, 80)"
                                               :trailWidth 1
                                               :svgStyle #js{:width "100%"
                                                             :height "100%"
                                                             :borderRadius "3px"}})]
               (dispatch [::events/add-progress-bar bar]))))}])

(defn main-panel []
  [:div (styles/app-container)
   [:div
    {:id "content-container"
     :class (styles/content-container)}
    [canvas-container]
    [:div
     {:id "progress-bar"
      :class (styles/progress-bar)}
     [progress-bar-line]
     [:div
      {:id "progress-controls"
       :class (styles/progress-controls)}
      [:div
       {:id "slide-controls"
        :class (styles/slide-controls)}
       [icon-prev {:size 24
                   :color "white"
                   :weight "bold"
                   :cursor "pointer"}]
       [:span {:id "slide-indicator"
               :class (styles/current-slide-indicator)}
        (let [{:keys [current-slide-index slide-count]} @(subscribe [::subs/slide-info])]
          (str current-slide-index " / " slide-count))]
       [icon-next {:size 24
                   :color "white"
                   :weight "bold"
                   :cursor "pointer"}]]
      [:div {:id "right-controls"
             :class (styles/right-controls)}
       [icon-control {:size 24
                      :color "white"
                      :cursor "pointer"}]
       [icon-smiley {:size 24
                     :color "white"
                     :cursor "pointer"}]
       [icon-chat {:size 24
                   :color "white"
                   :cursor "pointer"}]
       [icon-dots {:size 24
                   :color "white"
                   :weight "bold"
                   :cursor "pointer"}]]]]]])
