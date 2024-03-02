(ns immersa.ui.editor.slide-panel.styles
  (:require
    [immersa.ui.theme.colors :as colors]
    [immersa.ui.theme.typography :as typography]
    [spade.core :refer [defclass defattrs]]))

(def slides-panel-size "170px")

(defattrs side-bar []
  {:flex-shrink 0
   :width slides-panel-size
   ;; :border-right (str "1px solid " colors/panel-border)
   :box-sizing :border-box
   :box-shadow :none
   :display :flex
   :position :relative
   :flex-direction :column
   :align-items :center
   :gap "8px"})

(defclass add-slide-button []
  {:width "150px"
   :font-size typography/l})

(defclass slides-scroll-area []
  {:width slides-panel-size
   :height "100%"
   :overflow :hidden}
  ;; Fade out effect
  [:&:before
   {:content "''"
    :position "fixed"
    :width slides-panel-size
    :height "8px"
    :background "linear-gradient(180deg,#ffffff 0%,rgba(252,252,253,0) 100%)"}])

(defclass slide [camera-unlocked? selected?]
  {:width "123px"
   :height "70px"
   :outline :none
   :border-radius "5px"
   :border (cond
             (and camera-unlocked? selected?)
             (str "2px solid " colors/unlocked-camera)

             selected?
             (str "2px solid " colors/button-outline-border)

             :else (str "2px solid " colors/border2))}
  [:&:focus
   (when selected?
     {:filter "brightness(0.8)"})])

(defclass slide-img []
  {:width "100%"
   :height "100%"
   :box-sizing "border-box"
   :border "2px solid transparent"
   :border-radius "3px"})
