(ns immersa.ui.loading-screen
  (:require
    [immersa.ui.editor.components.progress :refer [progress-scene-loader]]
    [immersa.ui.subs :as subs]
    [re-frame.core :refer [subscribe]]))

(defn loading-screen [height]
  [:div {:style {:position "absolute"
                 :border-radius "15px"
                 :top "0"
                 :left "0"
                 :width "100%"
                 :height height
                 :background-color "black"
                 :z-index "9999"}}
   [:div {:style {:position "relative"
                  :display "flex"
                  :flex-direction "column"
                  :align-items "center"
                  :justify-content "center"
                  :gap "25px"
                  :top "50%"
                  :left "50%"
                  :transform "translate(-50%, -50%)"}}
    [:img {:src "img/logo_white.png"
           :style {:width "300px"}}]
    [progress-scene-loader @(subscribe [::subs/loading-progress])]]])
