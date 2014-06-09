(ns hara.coerce.map
  (:require [hara.protocol.map :refer :all])
  (:refer-clojure :exclude [get get-in] :as core))
  
(defn get
  "get"
  [obj k]
  (-get obj k))

(defn get-in
  "get-in"
  [obj ks]
  (-get-in obj ks))

(defn to
  "to"
  [obj]
  nil)

;;(defmulti classify (fn [m mta] (:class mta)))

;;(defmulti coerce (fn [cls m] cls))

;;(defmulti from (fn [cls m mta] cls))
