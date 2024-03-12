(ns immersa.ui.core
  (:require
    [applied-science.js-interop :as j]
    [breaking-point.core :as bp]
    [clojure.string :as str]
    [immersa.common.config :as config]
    [immersa.common.utils :as common.utils]
    [immersa.scene.api.core :as api]
    [immersa.ui.events :as events]
    [immersa.ui.views :as views]
    [re-frame.core :as re-frame]
    [reagent.dom :as rdom]))

(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/before-load before-load []
  (api/dispose-engine)
  (common.utils/remove-element-listeners))

(defn ^:dev/after-load mount-root [mode]
  (re-frame/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [views/main-panel mode] root-el)))

(defn init []
  (let [mode (if (str/starts-with? (j/get js/location :hostname) "present.")
               :present
               :editor)]
    (re-frame/dispatch-sync [::events/initialize-db mode])
    (re-frame/dispatch-sync [::bp/set-breakpoints {:breakpoints [:mobile 768
                                                                 :tablet 992
                                                                 :small-monitor 1200
                                                                 :large-monitor]}])
    (dev-setup)
    (mount-root mode)))
