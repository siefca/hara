(ns hara.protocol.map
  (:require [hara.protocol.constructor :as constructor]))

(defprotocol IMap
  (-to-map [obj])
  (-to-map-meta [obj]))

(defn to [obj])

(defmulti classify (fn [m mta] (:class mta)))

(defmulti coerce (fn [cls m] cls))



(defmulti from (fn [cls m mta] cls))
