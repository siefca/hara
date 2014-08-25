(ns hara.data.nested
  (:require [hara.common.checks :refer [hash-map?]]
            [hara.common.error :refer [suppress]]
            [hara.expression.shorthand :refer [check?->]]
            [clojure.set :as set]))

(defn keys-nested
  "The set of all nested keys in a map

  (keys-nested {:a {:b 1 :c {:d 1}}})
  => #{:a :b :c :d}"
  {:added "2.1"}
  ([m] (keys-nested m #{}))
  ([m ks]
     (if-let [[k v] (first m)]
       (cond (hash-map? v)
             (set/union
              (keys-nested (next m) (conj ks k))
              (keys-nested v))

             :else (recur (next m) (conj ks k)))
       ks)))

(defn merge-nested
  "Merges nested values from left to right.

  (merge-nested {:a {:b {:c 3}}} {:a {:b 3}})
  => {:a {:b 3}}

  (merge-nested {:a {:b {:c 1 :d 2}}}
                {:a {:b {:c 3}}})
  => {:a {:b {:c 3 :d 2}}}"
  {:added "2.1"}
  ([m] m)
  ([m1 m2]
     (if-let [[k v] (first m2)]
       (cond (nil? (get m1 k))
             (recur (assoc m1 k v) (dissoc m2 k))

             (and (hash-map? v) (hash-map? (get m1 k)))
             (recur (assoc m1 k (merge-nested (get m1 k) v)) (dissoc m2 k))

             (not= v (get m1 k))
             (recur (assoc m1 k v) (dissoc m2 k))

             :else
             (recur m1 (dissoc m2 k)))
       m1))
  ([m1 m2 & ms]
     (apply merge-nested (merge-nested m1 m2) ms)))

(defn merge-nil-nested
  "Merges nested values from left to right, provided the merged value does not exist

  (merge-nil-nested {:a {:b 2}} {:a {:c 2}})
  => {:a {:b 2 :c 2}}

  (merge-nil-nested {:b {:c :old}} {:b {:c :new}})
  => {:b {:c :old}}"
  {:added "2.1"}
  ([m] m)
  ([m1 m2]
     (if-let [[k v] (first m2)]
       (let [v1 (get m1 k)]
         (cond (nil? v1)
               (recur (assoc m1 k v) (next m2))

               (and (hash-map? v) (hash-map? v1))
               (recur (assoc m1 k (merge-nil-nested v1 v)) (next m2))

               :else
               (recur m1 (next m2))))
       m1))
  ([m1 m2 & more]
     (apply merge-nil-nested (merge-nil-nested m1 m2) more)))

(defn dissoc-nested
  "Returns `m` without all nested keys in `ks`.

  (dissoc-nested {:a {:b 1 :c {:b 1}}} [:b])
  => {:a {:c {}}}"
  {:added "2.1"}
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

(defn unique-nested
  "All nested values in `m1` that are unique to those in `m2`.

  (unique-nested {:a {:b 1}}
               {:a {:b 1 :c 1}})
  => {}

  (unique-nested {:a {:b 1 :c 1}}
               {:a {:b 1}})
  => {:a {:c 1}}"
  {:added "2.1"}
  ([m1 m2] (unique-nested m1 m2 {}))
  ([m1 m2 output]
     (if-let [[k v] (first m1)]
       (cond (nil? (get m2 k))
             (recur (dissoc m1 k) m2 (assoc output k v))

             (and (hash-map? v) (hash-map? (get m2 k)))
             (let [sub (unique-nested v (get m2 k))]
               (if (empty? sub)
                 (recur (dissoc m1 k) m2 output)
                 (recur (dissoc m1 k) m2 (assoc output k sub))))

             (not= v (get m2 k))
             (recur (dissoc m1 k) m2 (assoc output k v))

             :else
             (recur (dissoc m1 k) m2 output))
       output)))

(defn clean-nested
  "Returns a associative with nils and empty hash-maps removed.

   (clean-nested {:a {:b {:c {}}}})
   => {}

   (clean-nested {:a {:b {:c {} :d 1 :e nil}}})
   => {:a {:b {:d 1}}}"
  {:added "2.1"}
  ([m] (clean-nested m (constantly false) {}))
  ([m prchk] (clean-nested m prchk {}))
  ([m prchk output]
     (if-let [[k v] (first m)]
       (cond (or (nil? v) (suppress (check?-> m prchk)))
             (recur (dissoc m k) prchk output)

             (hash-map? v)
             (let [rmm (clean-nested v prchk)]
               (if (empty? rmm)
                 (recur (dissoc m k) prchk output)
                 (recur (dissoc m k) prchk (assoc output k rmm))))

             :else
             (recur (dissoc m k) prchk (assoc output k v)))
       output)))
