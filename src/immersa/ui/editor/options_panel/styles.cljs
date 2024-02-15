(ns immersa.ui.editor.options-panel.styles
  (:require
    [immersa.ui.theme.colors :as colors]
    [immersa.ui.theme.typography :as typography]
    [spade.core :refer [defclass defattrs]]))

(def header-height "57px")

(def slides-panel-size "170px")
(def options-panel-size "340px")

(def hover-style
  [:&:hover
   {:background colors/hover-bg
    :border-radius "5px"}])

(def active-style
  [:&:active
   {:background colors/active-bg
    :border-radius "5px"}])

(defattrs options-panel []
  {:width "340px"
   :z-index "5000"
   :display :flex
   :flex-direction :column
   :overflow :hidden
   :border-left (str "1px solid " colors/panel-border)
   :box-sizing :border-box})

(defclass options-panel-scroll-area []
  {:width options-panel-size
   :height "calc(100vh - 57px)"
   :overflow :hidden}
  ;; Fade out effect
  [:&:before
   {:content "''"
    :position "fixed"
    :width options-panel-size
    :height "8px"
    :background "linear-gradient(180deg,#ffffff 0%,rgba(252,252,253,0) 100%)"}])

(defattrs pos-rot-scale-comp-container [disabled?]
  {:display :flex
   :align-items :center
   :opacity (if disabled? "0.5" "1")
   :cursor (if disabled? :not-allowed :auto)
   :gap "8px"})

(defclass pos-rot-scale-comp-label []
  {:width "56px"
   :padding-right "5px"
   :user-select :none})

(defattrs color-picker-container []
  {:display :flex
   :flex-direction :column
   :gap "8px"})

(defattrs color-picker-button-container []
  {:display :flex
   :align-items :center
   :justify-content :space-between})

(defclass color-picker-button []
  {:border (str "1px solid " colors/border2)
   :border-radius "5px"
   :cursor :pointer
   :width "24px"
   :height "24px"}
  [:&:hover
   {:border (str "1px solid " colors/border3)
    :box-shadow (str colors/button-box-shadow " 0px 1px 2px")}])

(defclass color-picker-close-button []
  {:position :absolute
   :z-index 3
   :left "223px"})

(defattrs color-picker-component-container []
  {:position :relative})

(defattrs color-picker-component-wrapper []
  {:margin-top "1px"
   :user-select :none})
