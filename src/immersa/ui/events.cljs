(ns immersa.ui.events
  (:require
    [immersa.common.mixpanel :as mixpanel]
    [immersa.ui.crisp-chat :as crisp-chat]
    [immersa.ui.db :as db]
    [re-frame.core :refer [reg-event-db reg-event-fx reg-fx]]))

(reg-event-fx
  ::initialize-db
  (fn [_ [_ mode]]
    {:db db/default-db
     :fx [(when (= mode :editor)
            [::init-crisp-chat])]}))

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

(reg-fx
  ::init-crisp-chat
  (fn []
    (crisp-chat/init)))

(reg-fx
  :analytics
  mixpanel/track-with-debounce)
