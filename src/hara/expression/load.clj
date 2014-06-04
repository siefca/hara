(ns hara.expression.load
  (:require [hara.expression.form :refer [form-eval]])
  (:refer-clojure :exclude [load]))

(defn load-single
  "Perform a single step in the load process
  "
  {:added "2.1"}
  [m [k form]]
  (assoc m k (form-eval form m)))

(defn load
  "Seeds an initial map using forms

  (load {:a 1} [:b '(inc (:a %))
                :c '(+ (:a %) (:b %))]) 
  => {:a 1 :b 2 :c 3}"
  {:added "2.1"}
  ([m] (load m :_init))
  ([m init]
     (cond (keyword? init)
           (dissoc (load m (get m init)) init)

           (vector? init)
           (let [pairs (partition 2 init)]
             (reduce load-single m pairs)))))