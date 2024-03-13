(ns immersa.common.mixpanel
  (:require
    [applied-science.js-interop :as j]
    [goog.functions :as functions]
    [immersa.common.config :as config]))

(defn track [{:keys [event data]}]
  (when (and (not config/debug?)
             (not (vector? (js->clj js/mixpanel))))
    (.track js/mixpanel event (clj->js data))))

(def track-with-debounce (functions/debounce track 1000))

(defn init [user-id email full-name]
  (j/call js/mixpanel :init "968a825c18a21c0c27a40ae963f148a4" #js {:track_pageview true
                                                                    :persistence "localStorage"})
  (j/call js/mixpanel :identify user-id)
  (.set js/mixpanel.people (clj->js #js {:Email email
                                         :Name full-name})))
