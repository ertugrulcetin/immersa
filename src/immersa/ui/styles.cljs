(ns immersa.ui.styles
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
