(ns immersa.ui.editor.components.progress
  (:require
    ["@radix-ui/react-progress" :as Progress]
    [immersa.ui.theme.colors :as colors]
    [spade.core :refer [defclass]]))

(defclass progress-root []
  {:position :relative
   :overflow :hidden
   :background colors/black-a9
   :border-radius "5px"
   :width "300px"
   :height "5px"
   :transform "translateZ(0)"
   :-webkit-box-reflect "below 1px linear-gradient(transparent, #0005)"})

(defclass progress-indicator []
  {:background "linear-gradient(330deg, color(display-p3 0.523 0.318 0.751) 0px, color(display-p3 0.276 0.384 0.837) 100%)"
   :width "100%"
   :height "100%"
   :box-shadow "inset 0 0 10px 1px rgba(117,182,255,0.4), 0 0 20px rgba(117,182,255,0.1)"
   :transition "transform 660ms cubic-bezier(0.65, 0, 0.35, 1)"}
  [:&:before
   {:content "''"
    :position :absolute
    :inset 0
    :top 0
    :left 0}]
  [:&:after
   {:content "''"
    :position :absolute
    :filter "blur(10px)"
    :inset 0
    :top 0
    :left 0}])

(defn progress [progress]
  [:> Progress/Root
   {:class (progress-root)
    :value progress}
   [:> Progress/Indicator
    {:class (progress-indicator)
     :style {:transform (str "translateX(-" (- 100 progress) "%)")}}]])
