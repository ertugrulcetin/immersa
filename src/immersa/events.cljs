(ns immersa.events
  (:require
    [day8.re-frame.tracing :refer-macros [fn-traced]]
    [immersa.db :as db]
    [re-frame.core :as re-frame]))

(re-frame/reg-event-db
  ::initialize-db
  (fn-traced [_ _]
             db/default-db))
