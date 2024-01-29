(ns immersa.common.communication
  (:require
    [cljs.core.async :as a]))

(def event-bus (a/chan))
(def event-bus-pub (a/pub event-bus first))

(defn fire [event data]
  (a/put! event-bus [event data]))

