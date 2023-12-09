(ns immersa.events
  (:require
    [applied-science.js-interop :as j]
    [immersa.db :as db]
    [re-frame.core :refer [reg-event-db reg-event-fx reg-fx]]))

(reg-event-db
  ::initialize-db
  (fn [_ _]
    db/default-db))

(reg-event-db
  ::add-progress-bar
  (fn [db [_ bar]]
    (assoc db :progress-bar-element bar)))

(reg-event-fx
  ::update-slide-info
  (fn [{:keys [db]} [_ index slide-count]]
    (let [current-slide-index (inc index)]
      {:db (assoc db :current-slide-index current-slide-index
                  :slide-count slide-count)
       ::animate-progress-bar [(:progress-bar-element db) (/ current-slide-index slide-count)]})))

(reg-fx
  ::animate-progress-bar
  (fn [[progress-bar progress]]
    (j/call progress-bar :animate progress)))
