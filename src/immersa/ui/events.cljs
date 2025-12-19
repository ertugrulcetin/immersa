(ns immersa.ui.events
  (:require
    [immersa.ui.db :as db]
    [re-frame.core :refer [reg-event-db reg-event-fx reg-fx]]))

(reg-event-fx
  ::initialize-db
  (fn [_ [_ mode]]
    {:db db/default-db}))

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

;; Analytics effect - no-op for offline mode
(reg-fx
  :analytics
  (fn [_]
    ;; No-op: Analytics disabled in offline mode
    nil))
