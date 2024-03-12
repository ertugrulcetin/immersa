(ns immersa.ui.present.styles
  (:require
    [spade.core :refer [defglobal defclass defattrs]]))

(defclass content-container [editor-mode? present-loading?]
  {:width "100%"
   :height (cond
             editor-mode? "calc(100vh - 119px)"
             present-loading? "100vh"
             :else "calc(100vh - 64px)")
   :margin 0
   :padding 0
   :box-sizing "border-box"
   :display "flex"
   :flex-direction "column"
   :justify-content "center"
   :align-items "center"
   :background "radial-gradient(rgb(0,0,0), rgb(0,0,0))"
   :transition "background 1s ease-in-out"})

(defclass canvas-container []
  {:box-sizing "border-box"})

(defclass canvas []
  {:width "100%"
   :height "100%"
   ;; :border "1px solid red"
   :margin 0
   :padding 0
   :touch-action :none
   :display :block
   :outline :none})

(defclass progress-bar [present-loading?]
  {:display "flex"
   :flex-direction "column"
   :align-items "center"
   :position "fixed"
   :bottom "0"
   :left "0"
   :right "0"
   :width "100%"
   :height "64px"
   :visibility (if present-loading? "hidden" "visible")
   :background-color "#2a2c37"
   :box-shadow "inset 0 1px 0 rgb(63, 66, 80)"
   :box-sizing "border-box"
   :padding "4px 16px"})

(defclass progress-line []
  {:display :flex
   :width "100%"
   :padding-top "7px"
   :height "10px"})

(defclass progress-controls []
  {:display :flex
   :display-direction :row
   :justify-content :space-between
   :width "100%"
   :padding-top "7px"
   :height "15px"})

(defclass slide-controls []
  {:display :flex
   :flex-direction :row
   :align-items :center
   :margin-top "15px"
   :gap "10px"})

(defclass current-slide-indicator []
  {:-webkit-touch-callout :none
   :-webkit-user-select :none
   :-khtml-user-select :none
   :-moz-user-select :none
   :-ms-user-select :none
   :user-select :none
   :font-weight "bold"
   :color "white"
   :padding-bottom "2px"})

(defclass right-controls []
  {:display :flex
   :flex-direction :row
   :gap "10px"
   :align-items :center
   :height "30px"})

(defattrs arrow-keys-text []
  {:display :flex
   :flex-direction :column
   :align-items :center
   :justify-content :center
   :color :white
   :position :absolute
   :font-family "Open Sans, sans-serif"
   :top "65%"
   :left "50%"
   :transform "translate(-50%, -50%)"
   :user-select :none
   :z-index 5})

(defclass immersa-button []
  {:user-select :none
   :cursor :pointer
   :transition "all 0.3s ease"
   :width "120px"
   :height "100%"
   :border-radius "50px"
   :outline :none
   :display :flex
   :align-items :center
   :justify-content :center}
  [:&:hover {:transition "all 0.3s ease"}])

(defclass immersa-button-glow []
  [:&:hover {:box-shadow "rgba(111, 76, 255, 0.5) 0px 0px 20px 0px"}])

(defclass immersa-button-gradient-border []
  {:color "rgba(256, 256, 256)"
   :border "2px double transparent"
   :background-image "linear-gradient(rgb(13, 14, 33), rgb(13, 14, 33)), radial-gradient(circle at left top, rgb(1, 110, 218), rgb(217, 0, 192))"
   :background-origin "border-box"
   :background-clip "padding-box, border-box"})
