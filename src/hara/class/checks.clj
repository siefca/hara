(ns hara.class.checks)

(defn interface?
  "Returns `true` if `class` is an interface

  (interface? java.util.Map) => true
  (interface? Class) => false"
  {:added "2.1"}
  [class]
  (.isInterface class))

(defn abstract?
  "Returns `true` if `class` is an abstract class

  (abstract? java.util.Map) => true
  (abstract? Class) => false"
  {:added "2.1"}
  [class]
  (java.lang.reflect.Modifier/isAbstract (.getModifiers class)))

(defn multimethod?
  "Returns `true` if `obj` is a multimethod

  (multimethod? print-method) => true
  (multimethod? println) => false"
  {:added "2.1"}
  [obj]
  (instance? clojure.lang.MultiFn obj))

(defn protocol?
  "Returns `true` if `obj` is a protocol

  (defprotocol ISomeProtocol)
  (protocol? ISomeProtocol) => true

  (protocol? clojure.lang.ILookup) => false"
  {:added "2.1"}
  [obj]
  (and (instance? clojure.lang.PersistentArrayMap obj)
       (every? #(contains? obj %) [:on :on-interface :var])
       (-> obj :on str Class/forName class?)
       (-> obj :on-interface class?)))
