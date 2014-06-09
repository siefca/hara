(hara.common.extend)

(defn assoc-if
  ([m k v] (assoc-if m k v identity))
  ([m k v f]
     (if v
       (assoc m k (f v))
       m)))

(defn update-in-if
 ([m ks f & args]
    (if-let [v (get-in m ks)]
      (apply update-in m ks f args)
      m)))