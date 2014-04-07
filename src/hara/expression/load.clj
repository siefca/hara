(ns hara.expression.load
  (:require [hara.expression.form :refer [func-eval]])
  (:refer-clojure :exclude [load]))

(defn load-single [m [k form]]
  (assoc m k (func-eval form m)))

(defn load
  ([m] (load m :_init))
  ([m init]
     (cond (keyword? init)
           (dissoc (load m (get m init)) init)

           (vector? init)
           (let [pairs (partition 2 init)]
             (reduce load-single m pairs)))))