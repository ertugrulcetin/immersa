(ns immersa.ui.editor.components.slider
  (:require
    ["@radix-ui/react-slider" :as Slider]
    [immersa.ui.theme.colors :as colors]
    [spade.core :refer [defclass defattrs]]))

(defclass slider-root []
  {:position :relative
   :display :flex
   :align-items :center
   :user-select :none
   :touch-action :none
   :width "100%"
   :height "20px"})

(defclass slider-track []
  {:background-color colors/border2
   :position :relative
   :flex-grow 1
   :border-radius "9999px"
   :height "3px"})

(defclass slider-range []
  {:position :absolute
   :background-color colors/border2
   :border-radius "9999px"
   :height "100%"})

(defclass slider-thumb []
  {:display :block
   :width "12px"
   :height "12px"
   :background-color "white"
   :outline :none
   :border (str "1px solid " colors/border3)
   :border-radius "10px"}
  [:&:hover
   {:box-shadow (str colors/button-box-shadow " 0px 1px 2px")}]
  [:&:focus
   {:box-shadow (str colors/button-box-shadow " 0px 2px 3px")}])

(defn slider [{:keys [value min max step]
               :or {max 100
                    min 0
                    step 1
                    value 0}}]
  [:> Slider/Root
   {:class (slider-root)
    ;; :value [value]
    :min min
    :max max
    :step step}
   [:> Slider/Track
    {:class (slider-track)}
    [:> Slider/Range
     {:class (slider-range)}]]
   [:> Slider/Thumb
    {:class (slider-thumb)}]])
