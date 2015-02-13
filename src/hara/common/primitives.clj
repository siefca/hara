(ns hara.common.primitives
  (:require [hara.common.checks :refer [bytes?]]
            [hara.common.error :refer [error]]))

;; Constants

(defn T
  "Returns `true` for any combination of input `args`

  (T) => true
  (T :hello) => true
  (T 1 2 3) => true"
  {:added "2.0"}
  [& args] true)

(defn F
  "Returns `false` for any combination of input `args`

  (F) => false
  (F :hello) => false
  (F 1 2 3) => false"
  {:added "2.0"}
  [& args] false)

(defn NIL
  "Returns `nil` for any combination of input `args`

  (NIL) => nil
  (NIL :hello) => nil
  (NIL 1 2 3) => nil"
  {:added "2.0"}
  [& args] nil)
;; ## Constructors

(defn queue
  "Returns a `clojure.lang.PersistentQueue` object.

  (def a (queue 1 2 3 4))
  (pop a) => [2 3 4]"
  {:added "2.0"}
  ([] (clojure.lang.PersistentQueue/EMPTY))
  ([x] (conj (queue) x))
  ([x & xs] (apply conj (queue) x xs)))

(defn uuid
  "Returns a `java.util.UUID` object

  (uuid) =>  <random uuid>

  (uuid \"00000000-0000-0000-0000-000000000000\")
  => #uuid \"00000000-0000-0000-0000-000000000000\""
  {:added "2.0"}
  ([] (java.util.UUID/randomUUID))
  ([id]
     (cond (string? id)
           (java.util.UUID/fromString id)
           (bytes? id)
           (java.util.UUID/nameUUIDFromBytes id)
           :else (error id " can only be a string or byte array")))
  ([^Long msb ^Long lsb]
     (java.util.UUID. msb lsb)))

(defn instant
  "Returns a `java.util.Date` object

  (instant) =>  <current time>

  (instant 0) => #inst \"1970-01-01T00:00:00.000-00:00\""
  {:added "2.0"}
  ([] (java.util.Date.))
  ([^Long val] (java.util.Date. val)))

(defn uri
  "Returns a `java.net.URI` object

  (uri \"http://www.google.com\")
  => #(instance? java.net.URI %)"
  {:added "2.0"}
  [path] (java.net.URI/create path))

(defn class-array
  "Returns a Class array

  (let [^\"[Ljava.lang.String;\" a (class-array [\"a\" \"b\" \"c\"])]

    (type a) => (Class/forName \"[Ljava.lang.String;\")

    (aget a 0) => \"a\"

    (count a) => 3)"
  {:added "2.0"}
  ([seq] (class-array (-> seq first type) seq))
  ([type seq]
    (let [total (count seq)
          ^"[Ljava.lang.Object;" arr (make-array type total)]
      (doseq [i   (range total)]
        (aset arr i (nth seq i)))
      arr)))
