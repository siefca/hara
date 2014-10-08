(ns hara.reflect.common
  (:require [hara.common]))

(def ^:dynamic *cache* (atom {}))
  
(defn context-class
  "If x is a class, return x otherwise return the class of x

  (context-class String)
  => String

  (context-class \"\")
  => String"
  {:added "2.1"}
  [obj]
  (if (class? obj) obj (type obj)))

(defn combinations
  "find all combinations of `k` in a given input list `l`

  (combinations 2 [1 2 3])
  => [[2 1] [3 1] [3 2]]

  (combinations 3 [1 2 3 4])
  => [[3 2 1] [4 2 1] [4 3 1] [4 3 2]]"
  {:added "2.1"}
  [k l]
  (if (= 1 k) (map vector l)
      (apply concat
             (map-indexed
              #(map (fn [x] (conj x %2))
                    (combinations (dec k) (drop (inc %1) l)))
              l))))

(defn all-subsets
  "finds all non-empty sets of collection `s`

  (all-subsets [1 2 3])
  => [#{1} #{2} #{3} #{1 2} #{1 3} #{2 3} #{1 2 3}]"
  {:added "2.1"}
  [s]
  (apply concat
         (for [x (range 1 (inc (count s)))]
           (map #(into #{} %) (combinations x s)))))