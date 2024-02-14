(ns immersa.ui.editor.components.tooltip
  (:require
    ["@radix-ui/react-tooltip" :as Tooltip]
    [immersa.ui.editor.components.button :refer [shortcut-button]]
    [immersa.ui.editor.components.text :refer [text]]
    [spade.core :refer [defclass defkeyframes]]))

(defkeyframes slide-up-and-fade []
  [:from {:opacity 0
          :transform "translateY(2px)"}]
  [:to {:opacity 1
        :transform "translateY(0)"}])

(defkeyframes slide-right-and-fade []
  [:from {:opacity 0
          :transform "translateX(-2px)"}]
  :to {:opacity 1
       :transform "translateX(0)"})

(defkeyframes slide-down-and-fade []
  [:from {:opacity 0
          :transform "translateY(-2px)"}]
  [:to {:opacity 1
        :transform "translateY(0)"}])

(defkeyframes slide-left-and-fade []
  [:from {:opacity 0
          :transform "translateX(2px)"}]
  [:to {:opacity 1
        :transform "translateX(0)"}])

(defclass tooltip-root []
  {:all :unset})

(defclass text-content []
  {:line-height "1.5"})

(defclass tooltip-content []
  {:z-index "9999"
   :max-width "200px"
   :border-radius "4px"
   :padding "10px 15px"
   :font-size "15px"
   :line-height "1"
   :color "black"
   :background-color "white"
   :box-shadow "hsl(206 22% 7% / 35%) 0px 10px 38px -10px, hsl(206 22% 7% / 20%) 0px 10px 20px -15px"
   :user-select :none
   :animation-duration "400ms"
   :animation-timing-function "cubic-bezier(0.16, 1, 0.3, 1)"
   :will-change "transform, opacity"}
  ["&[data-state='delayed-open'][data-side='top']"
   {:animation-name [(slide-up-and-fade)]}]
  ["&[data-state='delayed-open'][data-side='right']"
   {:animation-name [(slide-left-and-fade)]}]
  ["&[data-state='delayed-open'][data-side='bottom']"
   {:animation-name [(slide-down-and-fade)]}]
  ["&[data-state='delayed-open'][data-side='left']"
   {:animation-name [(slide-right-and-fade)]}])

(defclass tooltip-arrow []
  {:fill :white})

(defn tooltip [{:keys [trigger content shortcuts delay]
                :or {delay 400}}]
  (let [shortcuts (and shortcuts (if (vector? shortcuts)
                                   shortcuts
                                   [shortcuts]))]
    [:> Tooltip/Provider {:delay-duration delay}
     [:> Tooltip/Root {:class (tooltip-root)}
      [:> Tooltip/Trigger {:as-child true}
       [:div trigger]]
      [:> Tooltip/Portal
       [:> Tooltip/Content {:class (tooltip-content) :sideOffset 5}
        [:div {:style {:display "flex"
                       :flex-direction "column"
                       :align-items "center"
                       :gap "4px"}}
         [text {:weight :light
                :size :s
                :class (text-content)}
          content]
         (when (seq shortcuts)
           [:div {:style {:display "flex"
                          :flex-direction "row"
                          :gap "4px"}}
            (for [s shortcuts]
              ^{:key s}
              [shortcut-button s])])]
        [:> Tooltip/Arrow {:class (tooltip-arrow)}]]]]]))
