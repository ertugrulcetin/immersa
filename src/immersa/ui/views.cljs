(ns immersa.ui.views
  (:require
    ["@clerk/clerk-react" :refer [ClerkProvider SignedIn SignedOut RedirectToSignIn]]
    [applied-science.js-interop :as j]
    [immersa.ui.editor.views :as editor.views]
    [immersa.ui.present.views :as present.views]
    [immersa.ui.theme.styles :as styles]))

(defn- editor []
  [:> ClerkProvider
   {:publishableKey "pk_test_ZGl2aW5lLXNhd2Zpc2gtMzcuY2xlcmsuYWNjb3VudHMuZGV2JA"}
   [:> SignedIn
    [:div (styles/app-container)
     [:f> editor.views/editor-panel]]]
   [:> SignedOut
    [:> RedirectToSignIn]]])

(defn main-panel []
  [editor]
  #_[:div (styles/app-container)
     ;; [present.views/present-panel]

     ])
