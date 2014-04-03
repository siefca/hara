(ns hara.collection
  (:require [clojure.set :as set]
            [hara.type-check :refer [hash-map? hash-set?]]))

 ;; ## Useful Methods

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

(defn manipulate
  "Higher order function for manipulating entire data
   trees. Clones refs and atoms. It is useful for type
   conversion and serialization/deserialization.
  "
 ([f x] (manipulate f x {}))
 ([f x cs]
    (let [m-fn    #(manipulate f % cs)
          pred-fn (fn [pd]
                    (cond (instance? Class pd) #(instance? pd %)
                          (fn? pd) pd
                          :else (constantly false)))
          custom? #((pred-fn (:pred %)) x)
          c (first (filter custom? cs))]
      (cond (not (nil? c))
            (let [ctor (or (:ctor c) identity)
                  dtor (or (:dtor c) identity)]
              (ctor (manipulate f (dtor x) cs)))

            :else
            (cond
              (instance? clojure.lang.Atom x)   (atom (m-fn @x))
              (instance? clojure.lang.Ref x)    (ref (m-fn @x))
              (instance? clojure.lang.Agent x)  (agent (m-fn @x))
              (list? x)                         (apply list (map m-fn x))
              (vector? x)                       (vec (map m-fn x))
              (instance? clojure.lang.IPersistentSet x)
              (set (map m-fn x))

              (instance? clojure.lang.IPersistentMap x)
              (zipmap (keys x) (map m-fn (vals x)))

              (instance? clojure.lang.ISeq x)
              (map m-fn x)

              :else (f x))))))

(defn deref-nested
  "Dereferences all nested refs within data-structures

    (deref-nested
         (atom {:a (atom {:b (atom :c)})}))
    => {:a {:b :c}}
 "
 ([x] (deref-nested identity x))
 ([f x] (deref-nested f x []))
 ([f x cs]
    (manipulate f
             x
             (conj cs {:pred clojure.lang.IDeref
                       :ctor identity
                       :dtor deref}))))
