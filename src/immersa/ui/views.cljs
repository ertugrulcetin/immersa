(ns immersa.ui.views
  (:require
    [immersa.ui.editor.views :as editor.views]
    [immersa.ui.present.views :as present.views]
    [immersa.ui.theme.styles :as styles]))

(defn main-panel []
  [:div (styles/app-container)
    [present.views/present-panel]
   ;[editor.views/editor-panel]
   ])
