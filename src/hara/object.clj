(ns hara.object
  (:require [hara.reflect :as reflect]
            [hara.protocol.map :as map]))

(defmulti from-map
  {:added "2.1"}
  (fn [data meta] (or (:type meta) meta)))

(defmethod from-map :default
  [data meta]
  (throw (Exception. (str "Not Implemented for: " meta " and " map)))

(defn to-map
  {:added "2.1"}
  [x]
  (map/-to-map x))

(defn to-meta
  {:added "2.1"}
  [x]
  (-to-map-meta x))

(extend-type nil
  IMap
  (-to-map [x] {})
  (-to-map-meta [x] {:type nil}))

(extend-type Object
  IMap
  (-to-map [x] (str x))
  (-to-map-meta [x] {:type (type x)}))
