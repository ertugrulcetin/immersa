(ns immersa.ui.subs
  (:require
    [re-frame.core :refer [reg-sub]]))

(reg-sub
  ::name
  (fn [db]
    (:name db)))

(reg-sub
  ::loading-screen?
  (fn [db]
    (:loading-screen? db)))

(reg-sub
  ::loading-progress
  (fn [db]
    (:loading-progress db)))
