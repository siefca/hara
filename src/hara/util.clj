(ns hara.utils
  (:require [clojure.string :as st]
            [clojure.set :as set]))


;; ## String Methods

(defn replace-all
  "Returns a string with all instances of `old` in `s` replaced with
   the value of `new`."
  [s old new]
  (.replaceAll s old new))

(defn starts-with
  "Returns `true` if `s` begins with `pre`."
  [s pre]
  (.startsWith s pre))

;; ## Type Predicates

(defn boolean?
  "Returns `true` if `x` is of type `java.lang.Boolean`."
  [x] (instance? java.lang.Boolean x))

(defn hash-map?
  "Returns `true` if `x` implements `clojure.lang.IPersistentMap`."
  [x] (instance? clojure.lang.IPersistentMap x))

(defn hash-set?
  "Returns `true` if `x` implements `clojure.lang.IPersistentHashSet`."
  [x] (instance? clojure.lang.PersistentHashSet x))

(defn long?
  "Returns `true` if `x` is of type `java.lang.Long`."
  [x] (instance? java.lang.Long x))

(defn bigint?
  "Returns `true` if `x` is of type `clojure.lang.BigInt`."
  [x] (instance? clojure.lang.BigInt x))

(defn bigdec?
  "Returns `true` if `x` is of type `java.math.BigDecimal`."
  [x] (instance? java.math.BigDecimal x))

(defn instant?
  "Returns `true` if `x` is of type `java.util.Date`."
  [x] (instance? java.util.Date x))

(defn uuid?
  "Returns `true` if `x` is of type `java.util.UUID`."
  [x] (instance? java.util.UUID x))

(defn uri?
  "Returns `true` if `x` is of type `java.net.URI`."
  [x] (instance? java.net.URI x))

(defn bytes?
  "Returns `true` if `x` is a primitive `byte` array."
  [x] (= (Class/forName "[B")
         (.getClass x)))

(defn type-checker
  "Returns the checking function associated with keyword `k`

     (type-checker :string)
     ;=> #'clojure.core/string?

     (type-checker :bytes)
     ;=> #'adi.utils/bytes?
   "
  [k]
  (resolve (symbol (str (name k) "?"))))

 ;; ## Misc Methods

(defn error
  "Throws an error when called. "
  ([e] (throw (Exception. (str e))))
  ([e & more]
     (throw (Exception. (apply str e more)))))

(defn func-map
  "Returns a hash-map `m`, with the the values of `m` being
   the items within the collection and keys of `m` constructed
   by mapping `f` to `coll`. This is used to turn a collection
   into a lookup for better search performance.

    (funcmap :id [{:id 1 :val 1} {:id 2 :val 2}])
    ;=> {1 {:id 1 :val 1} 2 {:id 2 :val 2}}
   "
  [f coll] (zipmap (map f coll) coll))

(defn remove-repeats
  "Returns a vector of the items in `coll` for which `(f item)` is unique
   for sequential `item`'s in `coll`.

    (remove-repeats [1 1 2 2 3 3 4 5 6])
    ;=> [1 2 3 4 5 6]

    (remove-repeats even? [2 4 6 1 3 5])
    ;=> [2 1]
   "
  ([coll] (remove-repeats identity coll))
  ([f coll] (remove-repeats f coll [] nil))
  ([f coll output last]
     (if-let [v (first coll)]
       (cond (and last (= (f last) (f v)))
             (recur f (next coll) output last)
             :else (recur f (next coll) (conj output v) v))
       output)))


(defn dissoc-in
  "Dissociates a key in a nested associative structure `m`, where `[k & ks]` is a
  sequence of keys. When `keep` is true, nested empty levels will not be removed.

    (dissoc-in {:a {:b {:c 3}}} [:a :b :c])
    ;=> {}

    (dissoc-in {:a {:b {:c 3}}} [:a :b :c] true)
    ;=> {:a {:b {}}}
  "
  ([m [k & ks]]
     (if-not ks
       (dissoc m k)
       (let [nm (dissoc-in (m k) ks)]
         (cond (empty? nm) (dissoc m k)
               :else (assoc m k nm)))))

  ([m [k & ks] keep]
     (if-not ks
       (dissoc m k)
       (assoc m k (dissoc-in (m k) ks keep)))))

(defn assocm
  ([m k v]
     (let [z (get m k)]
       (cond (nil? z) (assoc m k v)
             (hash-set? z)
             (cond (hash-set? v)
                   (assoc m k (set/union z v))

                   :else (assoc m k (conj z v)))

             :else
             (cond (hash-set? v)
                   (assoc m k (conj v z))

                   (= z v) m

                   :else (assoc m k (set [v z]))))))

  ([m cmp merge k v]))

(defn dissocm
  ([m k]
     (cond (coll? k)
           (let [[k v] k
                 z (get m k)]
             (cond (hash-set? z)
                   (let [hs (if (hash-set? v)
                              (set/difference z v)
                              (disj z v))]
                     (if (empty? hs)
                       (dissoc m k)
                       (assoc m k hs)))
                   :else
                   (if (or (= v z)
                           (and (hash-set? v)
                                (v z)))

                     (dissoc m k) m)))
           :else
           (dissoc m k))))
