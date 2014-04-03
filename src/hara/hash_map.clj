(ns hara.hash-map
  (:require [clojure.set :as set]
            [hara.fn :refer [pred->]]))

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

(defn assoc-if
  ([m k v]
    (if (nil? v) m
       (assoc m k v)))
  ([m k v & more]
    (apply assoc-if (assoc-if m k v) more)))

(defn assoc-in-if [m ks v]
  (if (nil? v) m
    (assoc-in m ks v)))

(defn assoc-nil
  ([m k v]
     (if (get m k) m (assoc m k v)))
  ([m k v & kvs]
     (apply assoc-nil (assoc-nil m k v) kvs)))

(defn assoc-nil-in
  [m ks v]
  (if (get-in m ks) m (assoc-in m ks v)))

(defn merge-nil
  ([m] m)
  ([m1 m2]
     (loop [m1 m1 m2 m2]
       (if-let [[k v] (first m2)]
         (if (get m1 k)
           (recur m1 (next m2))
           (recur (assoc m1 k v) (next m2)))
         m1)))
  ([m1 m2 & more]
     (apply merge-nil (merge-nil m1 m2) more)))

(defn merge-nil-nested
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
       (cond (nil? (get m2 k))
             (recur (dissoc m1 k) m2 (assoc output k v))

             (and (hash-map? v) (hash-map? (get m2 k)))
             (let [sub (diff-nested v (get m2 k))]
               (if (empty? sub)
                 (recur (dissoc m1 k) m2 output)
                 (recur (dissoc m1 k) m2 (assoc output k sub))))

             (not= v (get m2 k))
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
       (cond (nil? (get m1 k))
             (recur (assoc m1 k v) (dissoc m2 k))

             (and (hash-map? v) (hash-map? (get m1 k)))
             (recur (assoc m1 k (merge-nested (get m1 k) v)) (dissoc m2 k))

             (not= v (get m1 k))
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
  ([m prchk] (remove-nested m prchk {}))
  ([m prchk output]
     (if-let [[k v] (first m)]
       (cond (or (nil? v) (suppress (pred-> m prchk)))
             (recur (dissoc m k) prchk output)

             (hash-map? v)
             (let [rmm (remove-nested v prchk)]
               (if (empty? rmm)
                 (recur (dissoc m k) prchk output)
                 (recur (dissoc m k) prchk (assoc output k rmm))))

             :else
             (recur (dissoc m k) prchk (assoc output k v)))
       output)))