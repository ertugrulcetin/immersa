(ns immersa.ui.db
  (:require
    [immersa.presentations.basic :as basic]
    [immersa.presentations.schaltbau :as schaltbau]))

(def default-db
  {:name "re-frame"
   :mode :editor
   :editor {:scene-ready? false
            :slides {:current-index 0
                     :all []
                     :thumbnails {}}
            :gizmo {:position true
                    :rotation false
                    :scale false}}

   :present {:show-arrow-keys-text? true
             :show-pre-warm-text? false
             :background-color "rgb(0,0,0)"}})
