(ns immersa.ui.loading-screen
  (:require
    [immersa.ui.editor.components.progress :refer [progress]]
    [immersa.ui.subs :as subs]
    [re-frame.core :refer [subscribe]]))

(defn loading-screen [height]
  [:div {:style {:position "absolute"
                 :top "0"
                 :left "0"
                 :width "100%"
                 :height (str height "px")
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
    [:img {:src "img/logo.png"
           :style {:width "120px"
                   :height "120px"}}]
    [progress @(subscribe [::subs/loading-progress])]]])
