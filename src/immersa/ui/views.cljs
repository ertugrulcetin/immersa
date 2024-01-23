(ns immersa.ui.views
  (:require
    [immersa.ui.present.views :as present.views]
    [immersa.ui.styles :as styles]))

(defn main-panel []
  [:div (styles/app-container)
   [present.views/present-panel]])
