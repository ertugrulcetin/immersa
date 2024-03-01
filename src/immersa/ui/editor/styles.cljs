(ns immersa.ui.editor.styles
  (:require
    [immersa.ui.theme.colors :as colors]
    [immersa.ui.theme.typography :as typography]
    [spade.core :refer [defclass defattrs]]))

(def header-height "57px")
(def options-panel-size "340px")

(def hover-style
  [:&:hover
   {:background colors/hover-bg
    :border-radius "5px"}])

(def active-style
  [:&:active
   {:background colors/active-bg
    :border-radius "5px"}])

(defattrs editor-container []
  {:width "100%"
   :height "100%"
   :margin 0
   :padding 0
   :box-sizing "border-box"
   :display "flex"
   :flex 1
   :flex-direction "column"
   :overflow "hidden"
   :transition "background 1s ease-in-out"})

(defattrs content-container []
  {:display :flex
   :flex 1
   :overflow :hidden
   :position :relative})

(defclass canvas-container []
  {:box-sizing "border-box"})

(defclass canvas-wrapper []
  {:display :flex
   :flex 1
   :flex-direction :column
   :position :relative
   :overflow :hidden
   ;; TODO add those when show no UI
   ;; :justify-content "center"
   ;; :align-items "center"
   :box-sizing :border-box
   :box-shadow :none})

(defclass canvas []
  {:width "100%"
   :height "100%"
   :box-sizing "border-box"
   :border-radius "15px"
   :margin 0
   :padding 0
   :touch-action :none
   :display :block
   :outline :none})

(defattrs header-container []
  {:position :relative
   :display :flex
   :justify-content :space-between
   :padding "12px 16px"
   :width "100%"
   :height header-height
   :box-sizing :border-box})

(defattrs title-bar []
  {:display :flex
   :align-items :center
   :flex "0 1 50%"
   :overflow :hidden
   :min-width "24px"
   :margin-right "8px"})

(defattrs title-bar-full-width []
  {:max-width :none
   :display :block
   :overflow :hidden
   :white-space :nowrap
   :text-overflow :ellipsis
   :padding-left "8px"})

(defattrs title-container []
  {:font-weight "600"
   :display :flex
   :align-items :center
   :line-height "22px"
   :height "22px"
   :padding-right "2px"})

(defattrs title-label []
  {:overflow :hidden
   :font-size typography/l
   :font-weight typography/medium
   :color colors/text-primary
   :user-select :none
   :text-overflow :ellipsis
   :white-space :nowrap
   :margin-right "4px"})

(defattrs menubar-list-icon []
  {:display :flex
   :align-items :center
   :justify-content :center
   :outline :none
   :width "28px"
   :height "28px"}
  hover-style
  active-style)

(defattrs header-center-panel []
  {:justify-content :center
   :display :flex
   :align-items :center
   :position :relative
   :gap "16px"})

(defclass presentation-component [disabled?]
  {:display :flex
   :gap "2px"
   :width "45px"
   :height :auto
   :padding "5px"
   :user-select :none
   :opacity (if disabled? 0.5 1)
   :cursor (if disabled?
             :not-allowed
             :default)
   :flex-direction :column
   :justify-content :center
   :align-items :center}
  hover-style
  (when-not disabled? active-style))

(defclass presentation-component-cube []
  {:width (str "max-content !important")})

(defattrs header-right []
  {:flex "1 1 50%"
   :justify-content :flex-end
   :display :flex
   :align-items :center
   :position :relative})

(defattrs header-right-container []
  {:display :flex
   :gap "16px"})

(defattrs private-badge []
  {:display :flex
   :user-select :none
   :margin-left "4px"
   :align-items :center
   :border (str "1px solid " colors/border2)
   :color colors/text-primary
   :border-radius "10px"
   :gap "3px"
   :height "16px"
   :padding "0px 6px"})

(defattrs private-badge-label []
  {:font-size typography/s
   :font-weight "300"})

(defclass options-scroll-area []
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

(defclass present-share-width []
  {:width "88px"})

(defclass pos-rot-scale-comp-label []
  {:width "56px"
   :padding-right "5px"
   :user-select :none})
