(ns immersa.common.utils
  (:require
    [applied-science.js-interop :as j]))

(defonce db (atom {}))

(defn register-event-listener [element type f]
  (j/call element :addEventListener type f)
  (swap! db update :event-listeners (fnil conj []) [element type f])
  f)

(defn remove-element-listeners []
  (doseq [[element type f] (:event-listeners @db)]
    (j/call element :removeEventListener type f))
  (swap! db assoc :event-listeners [])
  (js/console.log "All events listeners removed"))
