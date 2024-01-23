(ns immersa.ui.subs
  (:require
    [re-frame.core :refer [reg-sub]]))

(reg-sub
  ::name
  (fn [db]
    (:name db)))
