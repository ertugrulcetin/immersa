(ns immersa.core
  (:require
    [breaking-point.core :as bp]
    [immersa.common.utils :as common.utils]
    [immersa.config :as config]
    [immersa.events :as events]
    [immersa.scene.api :as api]
    [immersa.views :as views]
    [re-frame.core :as re-frame]
    [reagent.dom :as rdom]))

(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/before-load before-load []
  (api/dispose-engine)
  (common.utils/remove-element-listeners))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [views/main-panel] root-el)))

(defn init []
  (re-frame/dispatch-sync [::events/initialize-db])
  (re-frame/dispatch-sync [::bp/set-breakpoints {:breakpoints [:mobile 768
                                                               :tablet 992
                                                               :small-monitor 1200
                                                               :large-monitor]
                                                 :debounce-ms 166}])
  (dev-setup)
  (mount-root))
