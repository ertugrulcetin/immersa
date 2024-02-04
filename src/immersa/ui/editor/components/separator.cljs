(ns immersa.ui.editor.components.separator
  (:require
    ["@radix-ui/react-separator" :as Separator]
    [immersa.ui.theme.colors :as colors]
    [spade.core :refer [defclass]]))

(defclass separator-style []
  {:background-color colors/panel-border}
  ["&[data-orientation='horizontal']"
   {:width "100%"
    :height "1px"}]
  ["&[data-orientation='vertical']"
   {:width "1px"
    :height "100%"}])

(defn separator
  ([]
   (separator {:orientation "horizontal"}))
  ([{:keys [style orientation]}]
   [:> Separator/Root {:decorative true
                       :orientation orientation
                       :style style
                       :class (separator-style)}]))
