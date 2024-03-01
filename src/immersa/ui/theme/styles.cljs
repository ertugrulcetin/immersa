(ns immersa.ui.theme.styles
  (:require
    [immersa.ui.theme.colors :as colors]
    [immersa.ui.theme.typography :as typography]
    [spade.core :refer [defglobal defclass defattrs]]))

(def defaults
  {:width "100%"
   :height "100%"
   :margin 0})

(def body
  (assoc defaults
         :background "#f0f0f0"
         :font-family typography/font
         :overscroll-behavior :none))

(def app
  {:border-radius "25px"
   :border "5px solid #f0f0f0"
   :box-sizing :border-box
   :background :white})

(defglobal theme
  [:html defaults]
  [":root"
   [:body body]
   [:div#app (merge defaults app)]

   [:h1 {:font-family typography/font}]
   [:h2 {:font-family typography/font}]
   [:h3 {:font-family typography/font}]
   [:h4 {:font-family typography/font}]
   [:h5 {:font-family typography/font}]
   [:button {:font-family typography/font}]
   [:textarea {:font-family typography/font}]

   {:*primary-color* colors/primary}])

(defattrs app-container []
  (assoc defaults
         :display :flex
         :flex-direction :column))
