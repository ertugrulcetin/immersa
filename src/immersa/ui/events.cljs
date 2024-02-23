(ns immersa.ui.events
  (:require
    [immersa.ui.db :as db]
    [re-frame.core :refer [reg-event-db reg-event-fx reg-fx]]))

(reg-event-db
  ::initialize-db
  (fn [_ _]
    db/default-db))

(reg-event-db
  ::show-loading-screen
  (fn [db _]
    (assoc db :loading-screen? true
           :loading-progress 0)))

(reg-event-db
  ::hide-loading-screen
  (fn [db _]
    (assoc db :loading-screen? false)))

(reg-event-db
  ::set-loading-progress
  (fn [db [_ progress]]
    (assoc db :loading-progress progress)))
