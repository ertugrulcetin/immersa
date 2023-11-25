(ns immersa.scene.macros
  (:refer-clojure :exclude [get assoc!])
  (:require
    [applied-science.js-interop :as j]
    [clojure.string :as str]))

(defmacro assoc! [obj & params]
  `(-> ~obj
       ~@(for [[k v] (partition 2 params)]
           `(j/assoc-in! ~(mapv keyword (str/split (name k) #"\.")) ~v))))

(defmacro get [o k & ks]
  (if (> (count ks) 0)
    `(j/get-in ~o ~(vec (cons k ks)))
    `(j/get ~o ~k)))

(defmacro cond-doto
  [expr & clauses]
  (assert (even? (count clauses)))
  (let [g (gensym)
        steps (map (fn [[test step]] `(if ~test (do (-> ~g ~step) ~g) ~g))
                   (partition 2 clauses))]
    `(let [~g ~expr
           ~@(interleave (repeat g) (butlast steps))]
       ~(if (empty? steps)
          g
          (last steps)))))
