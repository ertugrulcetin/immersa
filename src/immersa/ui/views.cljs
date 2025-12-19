(ns immersa.ui.views
  (:require
    [immersa.ui.editor.views :as editor.views]
    [immersa.ui.present.views :as present.views]
    [immersa.ui.theme.styles :as styles]))

(defn- editor []
  "Editor component - no authentication required for offline mode."
  [:div (styles/app-container)
   [:f> editor.views/editor-panel]])

(defn main-panel [mode]
  (if (= mode :editor)
    [editor]
    [:f> present.views/present-panel {:mode mode}]))
