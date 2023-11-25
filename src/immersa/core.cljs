(ns immersa.core
  (:require
    [immersa.config :as config]
    [immersa.events :as events]
    [immersa.views :as views]
    [re-frame.core :as re-frame]
    [immersa.scene.api :as api]
    [immersa.common.utils :as common.utils]
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
  (dev-setup)
  (mount-root))
