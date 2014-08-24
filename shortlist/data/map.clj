(ns hara.data.map)

(defn dissoc-in
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

(defn gap [m1 m2]
  (reduce (fn [i [k v]]
            (if (not= v (get m2 k))
              (assoc i k v)
              i))
          {} m1))

(defn assoc-if
  ([m k v]
     (if v (assoc m k v) m))
  ([m k v & more]
     (apply assoc-if (assoc-if m k v) more)))

(defn assoc-in-if
  [m arr v]
  (if v (assoc-in m arr v) m))

(defn update-in-if
  [m arr f & args]
  (if-let [v (get-in m arr)]
    (assoc-in m arr (apply f v args))
    m))

(defn merge-if
  ([m1 m2]
     (reduce (fn [i [k v]]
               (if v (assoc i k v) i))
             m1 m2))
  ([m1 m2 & more]
     (apply merge-if (merge-if m1 m2) more)))

(defn into-if
  [to from]
  (reduce (fn [i [k v :as e]]
            (if (or (and (coll? e) v)
                    (not (coll? e)))
              (conj i e)
              i))
          to from))

(defn select-keys-if
  [m ks]
  (reduce (fn [i k]
            (if-let [v (get m k)]
              (assoc m k v)
              m))
          m ks))

(defn merge-nil
  ([m1 m2]
     (reduce (fn [i [k v]]
               (if (get i k)
                 i
                 (assoc i k v)))
             m1 m2))
  ([m1 m2 & more]
     (apply merge-nil (merge-nil m1 m2) more)))

(defn assoc-nil
  ([m k v]
     (if (get m k) m (assoc m k v)))
  ([m k v & more]
     (apply (assoc-nil m k v) more)))

(defn assoc-in-nil
  [m ks v]
  (if (get-in m ks) m (assoc-in m ks v)))
