(ns hara.common
  (:require [clojure.string :as st]
            [clojure.set :as set])
  (:refer-clojure :exclude [send]))

;; ## Object Orientated Control

(defn call
  "Only executes `(f v1 ... vn)` if `f` is not nil"
  ([f] (if-not (nil? f) (f)) )
  ([f v] (if-not (nil? f) (f v)))
  ([f v1 v2] (if-not (nil? f) (f v1 v2)))
  ([f v1 v2 v3] (if-not (nil? f) (f v1 v2 v3)))
  ([f v1 v2 v3 v4 ] (if-not (nil? f) (f v1 v2 v3 v4)))
  ([f v1 v2 v3 v4 & vs] (if-not (nil? f) (apply f v1 v2 v3 v4 vs))))

(defn msg
  "Object oriented style dispatch."
  ([obj kw] (call (obj kw) obj))
  ([obj kw v] (call (obj kw) obj v))
  ([obj kw v1 v2] (call (obj kw) obj v1 v2))
  ([obj kw v1 v2 v3] (call (obj kw) obj v1 v2 v3))
  ([obj kw v1 v2 v3 v4] (call (obj kw) obj v1 v2 v3 v4))
  ([obj kw v1 v2 v3 v4 & vs] (apply call (obj kw) obj v1 v2 v3 v4 vs)))

(defn make-%? [f args]
  (apply list 'fn ['?%]
         (list (concat [f '?%] `[~@args]))))

(defmacro %? [f & args]
  (make-?hash f args))

(defn functify [chk obj]
  true)

(defn eq-chk
  "Returns `true` when `v` equals `chk`, or if `chk` is a function, `(chk v)`

      (eq-chk 2 2) ;=> true

      (eq-chk 2 even?) ;=> true
    "
  [obj chk]
  (or (= obj chk)
      (and (ifn? chk)
           (list? chk) (functify chk obj)
           (-> (chk obj) not not))))

(defn get-sel [obj sel]
  "Provides a shorthand way of getting a return value.
   `sel` can be a function, a vector, or a value.

    (get-sel {:a {:b {:c 1}}} :a) => {:b {:c 1}}

    (get-sel {:a {:b {:c 1}}} [:a :b]) => {:c 1}
  "
  (cond (vector? sel) (get-in obj sel)
        (ifn? sel) (sel obj)
        :else (get obj sel)))

(defn sel-chk
  "Returns `true` if `(sel obj)` satisfies `eq-chk`

    (sel-chk {:a {:b 1}} :a hash-map?) ;=> true

    (sel-chk {:a {:b 1}} [:a :b] 1) ;=> true
  "
  [obj sel chk]
  (eq-chk (get-sel obj sel) chk))

(defn sel-chk-all
  "Returns `true` if `obj` satisfies all pairs of sel and chk

    (sel-chk-all {:a {:b 1}} [:a {:b 1}] [:a hash-map?]) => true
  "
  [obj scv]
  (every? (fn [[sel chk]]
            (sel-chk obj sel chk))
          (partition 2 scv)))

(defn eq-sel
  "A shortcut to compare if two vals are equal.

      (eq-sel {:id 1 :a 1} {:id 1 :a 2} :id)
      ;=> true

      (eq-sel {:db {:id 1} :a 1} {:db {:id 1} :a 2} [:db :id])
      ;=> true
  "
  [obj1 obj2 sel]
  (= (get-sel obj1 sel) (get-sel obj2 sel)))

(defn eq-pri
  "Shorthand ways of checking where `m` fits `pri`

    (eq-pri {:a 1} :a) ;=> truthy

    (eq-pri {:a 1 :val 1} [:val 1]) ;=> true

    (eq-pri {:a {:b 1}} [[:a :b] odd?]) ;=> true
  "
  [obj pri]
  (cond (vector? pri)
        (sel-chk-all obj pri)

        :else
        (eq-chk obj pri)))

;; ## Exceptions

(defn error
  "Throws an exception when called.

    (error \"This is an error\")
    ;=> (throws Exception)
  "
  ([e] (throw (Exception. (str e))))
  ([e & more]
     (throw (Exception. (apply str e more)))))

(defmacro suppress
  "Suppresses any errors thrown.

    (suppress (error \"Error\"))
    ;=> nil
  "
  [& body]
  `(try ~@body (catch Throwable ~'t)))

(defn suppress-pri [obj pri res]
  (suppress (if (eq-pri obj pri) res)))


;; ## String Methods

(defn replace-all
  "Returns a string with all instances of `old` in `s` replaced with
   the value of `new`.

    (h/replace-all \"hello there, hello again\"
                   \"hello\" \"bye\")
    ;=> \"bye there, bye again\"
  "
  [s old new]
  (.replaceAll s old new))

(defn starts-with
  "Returns `true` if `s` begins with `pre`.

    (h/starts-with \"prefix\" \"pre\") ;=> true

    (h/starts-with \"prefix\" \"suf\") ;=> false
  "
  [s pre]
  (.startsWith s pre))

;; ## Type Predicates

(defn boolean?
  "Returns `true` if `x` is of type `java.lang.Boolean`.

    (boolean? false) ;=> true
  "
  [x] (instance? java.lang.Boolean x))

(defn hash-map?
  "Returns `true` if `x` implements `clojure.lang.IPersistentMap`.

    (hash-map? {}) ;=> true
  "
  [x] (instance? clojure.lang.IPersistentMap x))

(defn hash-set?
  "Returns `true` if `x` implements `clojure.lang.IPersistentHashSet`.

    (hash-set? #{}) ;=> true
  "
  [x] (instance? clojure.lang.PersistentHashSet x))

(defn long?
  "Returns `true` if `x` is of type `java.lang.Long`.

    (h/long? 1) ;=> true

    (h/long? 1N) ;=> false
  "
  [x] (instance? java.lang.Long x))

(defn bigint?
  "Returns `true` if `x` is of type `clojure.lang.BigInt`.

    (h/bigint? 1N) ;=> true
  "
  [x] (instance? clojure.lang.BigInt x))

(defn bigdec?
  "Returns `true` if `x` is of type `java.math.BigDecimal`.

     (h/bigdec? 1M) ;=> true
  "
  [x] (instance? java.math.BigDecimal x))

(defn instant?
  "Returns `true` if `x` is of type `java.util.Date`.

    (instant? (instant 0)) => true
  "
  [x] (instance? java.util.Date x))

(defn uuid?
  "Returns `true` if `x` is of type `java.util.UUID`.

    (uuid? (uuid)) ;=> true
  "
  [x] (instance? java.util.UUID x))

(defn uri?
  "Returns `true` if `x` is of type `java.net.URI`.

    (uri? (uri \"http://www.google.com\"))
    ;=> true
  "
  [x] (instance? java.net.URI x))

(defn bytes?
  "Returns `true` if `x` is a primitive `byte` array.

    (bytes? (byte-array 8)) ;=> true

  "
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


;; ## Constructors

(defn queue
  "Returns a `clojure.lang.PersistentQueue` object.

    (def a (queue 1 2 3 4))
    (seq (pop a) ;=> [2 3 4]
  "
  ([] (clojure.lang.PersistentQueue/EMPTY))
  ([x] (conj (queue) x))
  ([x & xs] (apply conj (queue) x xs)))

(defn uuid
  "Returns a `java.util.UUID` object

    (uuid) ;=> <random uuid>

    (uuid \"00000000-0000-0000-0000-000000000000\")
    ;=> #uuid \"00000000-0000-0000-0000-000000000000\"
  "
  ([] (java.util.UUID/randomUUID))
  ([id]
     (cond (string? id)
           (java.util.UUID/fromString id)
           (bytes? id)
           (java.util.UUID/nameUUIDFromBytes id)
           :else (error id " can only be a string or byte array")))
  ([^java.lang.Long msb ^java.lang.Long lsb]
     (java.util.UUID. msb lsb)))

(defn instant
  "Returns a `java.util.Date` object

    (instant) ;=> <current time>

    (instant 0) ;=> 1970-01-01T00:00:00.000-00:00
  "
  ([] (java.util.Date.))
  ([val] (java.util.Date. val)))

(defn uri
  "Returns a `java.net.URI` object

    (uri \"http://www.google.com\")
    ;=> #<URI http://www.google.com>
  "
  [path] (java.net.URI/create path))

 ;; ## Misc Methods

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

(defn replace-walk
  "Replaces all values in coll with the replacements defined as a lookup

    (replace-walk {:a 1 :b {:c 1}} {1 2})
    ;=> {:a 2 :b {:c 2}}
  "
  [coll rep]
  (cond (vector? coll)   (mapv #(replace-walk % rep) coll)
        (list? coll)     (apply list (map #(replace-walk % rep) coll))
        (hash-set? coll) (set (map #(replace-walk % rep) coll))
        (hash-map? coll) (zipmap (keys coll)
                               (map #(replace-walk % rep) (vals coll)))
        (rep coll) (rep coll)
        :else coll))

(defn group-bys
  "Returns a map of the elements of coll keyed by the result of
  f on each element. The value at each key will be a set of the
  corresponding elements, in the order they appeared in coll.

    (group-bys even? [1 2 3 4 5])
    ;=> {false #{1 3 5}, true #{2 4}}
  "
  [f coll]
  (persistent!
   (reduce
    (fn [ret x]
      (let [k (f x)]
        (assoc! ret k (conj (get ret k #{}) x))))
    (transient {}) coll)))

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


;; ## Exceptions
(defn keys-nested
  "Return the set of all nested keys in `m`.

    (keys-nested {:a {:b 1 :c {:d 1}}})
    ;=> #{:a :b :c :d})"
  ([m] (keys-nested m #{}))
  ([m ks]
     (if-let [[k v] (first m)]
       (cond (hash-map? v)
             (set/union
              (keys-nested (next m) (conj ks k))
              (keys-nested v))

             :else (recur (next m) (conj ks k)))
       ks)))

(defn dissoc-nested
   "Returns `m` without all nested keys in `ks`.

    (dissoc-nested {:a {:b 1 :c {:b 1}}} [:b])
    ;=> {:a {:c {}}}"

  ([m ks] (dissoc-nested m (set ks) {}))
  ([m ks output]
    (if-let [[k v] (first m)]
      (cond (ks k)
            (recur (next m) ks output)

            (hash-map? v)
            (recur (next m) ks
              (assoc output k (dissoc-nested v ks)))
            :else
            (recur (next m) ks (assoc output k v)))
      output)))

(defn diff-nested
  "Returns any nested values in `m1` that are different to those in `m2`.

    (diff-nested {:a {:b 1}}
                 {:a {:b 1 :c 1}})
    ;=> {}

    (diff-nested {:a {:b 1 :c 1}}
                 {:a {:b 1}})
    ;=> {:a {:c 1}}"
  ([m1 m2] (diff-nested m1 m2 {}))
  ([m1 m2 output]
     (if-let [[k v] (first m1)]
       (cond (nil? (k m2))
             (recur (dissoc m1 k) m2 (assoc output k v))

             (and (hash-map? v) (hash-map? (k m2)))
             (let [sub (diff-nested v (k m2))]
               (if (empty? sub)
                 (recur (dissoc m1 k) m2 output)
                 (recur (dissoc m1 k) m2 (assoc output k sub))))

             (not= v (k m2))
             (recur (dissoc m1 k) m2 (assoc output k v))

             :else
             (recur (dissoc m1 k) m2 output))
       output)))

(defn merge-nested
  "Merges all nested values in `m1`, `m2` and `ms` if there are more.
   nested values of maps on the right will replace those on the left if
   the keys are the same.

    (merge-nested {:a {:b {:c 3}}} {:a {:b 3}})
    ;=> {:a {:b 3}}

    (merge-nested {:a {:b {:c 1 :d 2}}}
                  {:a {:b {:c 3}}})
    ;=> {:a {:b {:c 3 :d 2}}})
  "
  ([m] m)
  ([m1 m2]
  (if-let [[k v] (first m2)]
    (cond (nil? (k m1))
          (recur (assoc m1 k v) (dissoc m2 k))

          (and (hash-map? v) (hash-map? (k m1)))
          (recur (assoc m1 k (merge-nested (k m1) v)) (dissoc m2 k))

          (not= v (k m1))
          (recur (assoc m1 k v) (dissoc m2 k))

          :else
          (recur m1 (dissoc m2 k)))
    m1))
  ([m1 m2 m3 & ms]
     (apply merge-nested (merge-nested m1 m2) m3 ms)))

(defn remove-nested
  "Returns a associative with nils and empty hash-maps removed.

    (remove-nested {:a {:b {:c {}}}})
    ;=> {}

    (remove-nested {:a {:b {:c {} :d 1}}})
    ;=> {:a {:b {:d 1}}}
  "
  ([m] (remove-nested m (constantly false) {}))
  ([m pri] (remove-nested m pri {}))
  ([m pri output]
     (if-let [[k v] (first m)]
       (cond (or (nil? v) (suppress (eq-pri m pri)))
             (recur (dissoc m k) pri output)

             (hash-map? v)
             (let [rmm (remove-nested v pri)]
               (if (empty? rmm)
                 (recur (dissoc m k) pri output)
                 (recur (dissoc m k) pri (assoc output k rmm))))

             :else
             (recur (dissoc m k) pri (assoc output k v)))
       output)))


;; Reference methods

(defn hash-keyword
  [this & ids]
  (keyword (str "__" (st/join "_" (cons (.hashCode this) (map name ids))) "__")))

(defn atom? [obj] (instance? clojure.lang.Atom obj))
(defn ref? [obj]  (instance? clojure.lang.Ref obj))

(defn set-iref! [rf obj]
  (cond (atom? rf) (reset! rf obj)
        (ref? rf) (dosync (ref-set rf obj))))

(defn follow
  ([master slave id] (follow master slave id identity))
  ([master slave id f]
     (add-watch master (hash-keyword slave id)
                (fn [_ _ _ v]
                  (set-iref! slave (f v))))))

(defn unfollow [master slave id]
  (remove-watch master (hash-keyword slave id)))

(defn manipulate*
 ([f x] (manipulate* f x {}))
 ([f x cs]
    (let [m-fn    #(manipulate* f % cs)
          pred-fn (fn [pd]
                    (cond (instance? Class pd) #(instance? pd %)
                          (fn? pd) pd
                          :else (constantly false)))
          custom? #((pred-fn (:pred %)) x)
          c (first (filter custom? cs))]
      (cond (not (nil? c))
            (let [ctor (or (:ctor c) identity)
                  dtor (or (:dtor c) identity)]
              (ctor (manipulate* f (dtor x) cs)))

            :else
            (cond
              (instance? clojure.lang.Atom x)   (atom (m-fn @x))
              (instance? clojure.lang.Ref x)    (ref (m-fn @x))
              (instance? clojure.lang.Agent x)  (agent (m-fn @x))
              (list? x)                         (apply list (map m-fn x))
              (vector? x)                       (vec (map m-fn x))

              (instance? clojure.lang.ISeq x)
              (map m-fn x)

              (instance? clojure.lang.IPersistentSet x)
              (set (map m-fn x))

              (instance? clojure.lang.IPersistentMap x)
              (into {} (map m-fn x))

              :else (f x))))))

(defn deref*
 ([x] (deref* identity x))
 ([f x] (deref* f x []))
 ([f x cs]
    (manipulate* f
                 x
                 (conj cs {:pred clojure.lang.IDeref
                           :ctor identity
                           :dtor deref}))))
