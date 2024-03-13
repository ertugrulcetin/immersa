(ns immersa.common.mixpanel
  (:require
    [applied-science.js-interop :as j]
    [goog.functions :as functions]
    [immersa.common.config :as config]))

(def track
  (functions/debounce
    (fn [{:keys [event data]}]
      (when (and (not config/debug?)
                 (not (vector? (js->clj js/mixpanel))))
        (.track js/mixpanel event (clj->js data))))
    1000))

(defn init [user-id]
  (j/call js/mixpanel :init "968a825c18a21c0c27a40ae963f148a4" #js {:track_pageview true
                                                                    :persistence "localStorage"})
  (j/call js/mixpanel :identify user-id))
