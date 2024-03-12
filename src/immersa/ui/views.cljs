(ns immersa.ui.views
  (:require
    ["@clerk/clerk-react" :refer [ClerkProvider SignedIn SignedOut RedirectToSignIn]]
    [immersa.common.config :as config]
    [immersa.ui.editor.views :as editor.views]
    [immersa.ui.present.views :as present.views]
    [immersa.ui.theme.styles :as styles]))

(defn- editor []
  [:> ClerkProvider
   {:publishableKey (if config/debug?
                      "pk_test_ZGl2aW5lLXNhd2Zpc2gtMzcuY2xlcmsuYWNjb3VudHMuZGV2JA"
                      "pk_live_Y2xlcmsuaW1tZXJzYS5hcHAk")}
   [:> SignedIn
    [:div (styles/app-container)
     [:f> editor.views/editor-panel]]]
   [:> SignedOut
    [:> RedirectToSignIn]]])

(defn main-panel [mode]
  (if (= mode :editor)
    [editor]
    [:f> present.views/present-panel {:mode mode}]))
