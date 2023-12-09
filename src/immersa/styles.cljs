(ns immersa.styles
  (:require
    [spade.core :refer [defglobal defclass defattrs]]))

(def defaults
  {:width "100%"
   :height "100%"
   :margin 0})

(def body
  (assoc defaults
         :font-family "'Open Sans', sans-serif"))

(defglobal html+body+app
  [:html (assoc defaults :background-color :white)]
  [:body body]
  [:div#app defaults])

(defattrs app-container []
  (assoc defaults
         :display :flex
         :flex-direction :column))

(defclass content-container []
  {:width "100%"
   :height "calc(100vh - 64px)"
   :margin 0
   :padding 0
   :box-sizing "border-box"
   :display "flex"
   :justify-content "center"
   :align-items "center"
   :background-color "rgb(13, 14, 19)"})

(defclass canvas-container [{:keys [width height]}]
  {:width (str width "px")
   :height (str height "px")
   :box-sizing "border-box"})

(defclass canvas []
  {:width "100%"
   :height "100%"
   ;; :border "1px solid red"
   :margin 0
   :padding 0
   :touch-action :none
   :display :block
   :outline :none})

(defclass progress-bar []
  {:display "flex"
   :flex-direction "column"
   :align-items "center"
   :position "fixed"
   :bottom "0"
   :left "0"
   :right "0"
   :width "100%"
   :height "64px"
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
  {:font-weight "bold"
   :color "white"
   :padding-bottom "2px"})

(defclass right-controls []
  {:display :flex
   :flex-direction :row
   :gap "10px"})
