(ns hara.class.inheritance
  (:require [clojure.set :as set])
  (:refer-clojure :exclude [list]))

(defn list
  "Lists the direct ancestors of a class
  (inheritance/list clojure.lang.PersistentHashMap)
  => [clojure.lang.PersistentHashMap
      clojure.lang.APersistentMap
      clojure.lang.AFn
      java.lang.Object]"
  {:added "2.1"}
  ([cls] (list cls []))
  ([cls output]
     (if (nil? cls)
       output
       (recur (.getSuperclass cls) (conj output cls)))))

(defn tree
  "Lists the hierarchy of bases and interfaces of a class.
  (inheritance/tree Class)
  => [[java.lang.Object #{java.io.Serializable
                          java.lang.reflect.Type
                          java.lang.reflect.AnnotatedElement
                          java.lang.reflect.GenericDeclaration}]]
  "
  {:added "2.1"}
  ([cls] (tree cls []))
  ([cls output]
     (let [base (.getSuperclass cls)]
       (if-not base output
               (recur base
                      (conj output [base (-> (.getInterfaces cls) seq set)]))))))


(defn best-match
  "finds the best matching interface or class from a list of candidates

  (best-match #{Object} Long) => Object
  (best-match #{String} Long) => nil
  (best-match #{Object Number} Long) => Number"
  {:added "2.1"}
  [candidates ^Class class]
  (or (get candidates class)
      (->> (apply concat (tree class))
           (map (fn [v]
                  (if (set? v)
                    (first (set/intersection v candidates))
                    (get candidates v))))
           (filter identity)
           first)))
