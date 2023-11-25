(ns immersa.styles
  (:require
    [spade.core :refer [defglobal defclass defattrs]]))

(defglobal body
  [:body
   {:height "100%"
    :width "100%"
    :margin "0px"
    :background-color :white}])

(defglobal html
  [:html
   {:height "100%"
    :width "100%"
    :margin "0px"
    :background-color :white}])

(defglobal app
  [:div#app
   {:height "100%"
    :width "100%"
    :margin "0px"
    :background-color :white}])

(defattrs app-container []
  {:height "100%"
   :width "100%"
   :margin "0px"
   :background-color :white
   :display "flex"
   :flex-direction "column"})

(defattrs toolbar
  []
  {:height "60px" ; Adjust as per your design
   :width "100%"
   :background-color "#f0f0f0" ; Light grey color for the toolbar
   :display "flex"
   :justify-content "space-between"
   ;; :padding "0 20px" ;; Padding on the sides
   :box-shadow "0 2px 4px rgba(0,0,0,0.1)" ; Optional shadow for depth
   :z-index "10"})

;; The main content area
(defattrs content []
  {:display "flex"
   :flex-direction "row"
   :overflow "hidden"
   ;; :width "100%"
   ;; :max-width "1440px"
   :height "100%" ; Assuming a toolbar height of 60px
   ;; :margin-top "60px" ;; Assuming a toolbar height of 60px
   :position "relative"})

;; The sidebar on the left
(defattrs sidebar
  []
  {:flex "0 0 170px" ; Fixed width for the sidebar
   :background-color "#e8e8e8"
   ;; :border "1px solid orange"
   })

;; The options bar on the right
(defattrs options-bar
  []
  {:flex "0 0 170px" ; Fixed width for the options bar
   :background-color "#e8e8e8"})

;; The canvas container in the middle
(defattrs canvas-container []
  {:flex-grow "1"
   :flex-shrink "1"
   ;; :border "1px solid green"
   ;; :flex-basis "calc(100% - 420px)" ;; Total width minus the width of both sidebars
   :background-color "#fff"
   :position "relative"
   :display "flex"
   :justify-content "center"
   :flex-direction "column"
   :align-items "center"})

(defattrs canvas-wrapper []
  {:position :relative
   :width "100%"
   :height "100%"})

(defclass canvas []
  {:position :absolute
   :outline :none
   :top "0"
   :left "0"
   :width "100%" ; Full width of the container
   ;; :aspect-ratio "1.77/1" ; Maintain a 1:1 aspect ratio
   :border "1px solid red"
   :background "#e9e9e9"})

(defattrs canvas-footer []
  {:width "100%"
   #_#_:border "1px solid blue"})
