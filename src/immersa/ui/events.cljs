(ns immersa.ui.events
  (:require
    [immersa.ui.db :as db]
    [re-frame.core :refer [reg-event-db reg-event-fx reg-fx]]))

(reg-event-db
  ::initialize-db
  (fn [_ _]
    db/default-db))
