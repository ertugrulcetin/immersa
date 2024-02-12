(ns immersa.ui.db
  (:require
    [immersa.presentations.schaltbau :as schaltbau]))

(def default-db
  {:name "re-frame"
   :mode :editor
   :editor {:slides {:current-index 0
                     :all schaltbau/slides
                     :thumbnails schaltbau/thumbnails}
            :gizmo {:position true
                    :rotation false
                    :scale false}}

   :present {:show-arrow-keys-text? true
             :show-pre-warm-text? false
             :background-color "rgb(0,0,0)"}})
