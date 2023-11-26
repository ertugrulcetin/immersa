(ns immersa.styles
  (:require
    [spade.core :refer [defglobal defclass defattrs]]))

(def defaults
  {:overflow :hidden
   :width "100%"
   :height "100%"
   :margin "0px"
   :display :block})

(defglobal html+body+app
  [:html (assoc defaults :background-color :white)]
  [:body defaults]
  [:div#app defaults])

(defattrs app-container []
  (assoc defaults
         :display :flex
         :flex-direction :column))

(defclass canvas []
  {:width "100%"
   :height "100%"
   :touch-action :none
   :display :block
   :outline :none})

