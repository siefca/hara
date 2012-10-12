(ns hara.data.dyna
  (:use [hara.data.iotam :only [iswap! ireset!]])
  (:import hara.data.DynaRec))

(defn new
  ([] (DynaRec.))
  ([v & [ks]]
     (let [ks (or ks (-> v first keys))
           dk (DynaRec. ks)]
       (dosync
        (doseq [e v]
          (conj! dk e)))
       dk)))

(defn- $ [dk] (:data (.state dk)))

(defn ids [dk] (keys dk))

(defn has-id? [dk id] (contains? @dk id))

(defn ch-id! [dk oid nid]
  {:pre [(has-id? dk oid)
         (or (not (has-id? dk nid))
             (= oid nid))]}
  (let [ne  (assoc @(dk oid) :id nid)]
    (dosync
     (dissoc! dk oid)
     (conj! dk ne))))

(defn search
  ([dk]
     (search dk (fn [_] true)))
  ([dk pred]
     (search dk
             pred
             (fn [x y] (.compareTo (:id x) (:id y)))))
  ([dk pred comp]
     (let [pred (if (fn? pred)
                  pred
                  #(= pred (:id %)))]
       (->> (ids dk)
            (map #(dk %))
            (map deref)
            (filter pred)
            (sort comp)))))

(defn select
  [dk id]
  {:pre [(has-id? dk id)]}
  @(dk id))

(defn empty! [dk]
  (doseq [w (.getWatches dk)]
    (remove-watch dk w))
  (dosync (alter ($ dk) empty) dk))

(defn delete! [dk id]
  {:pre [(has-id? dk id)]}
  (dosync (dissoc! dk id)))

(defn insert! [dk e]
  {:pre [(not (has-id? dk (:id e)))]}
  (dosync (conj! dk e)))

(defn update! [dk e]
  (cond (has-id? dk (:id e))  (iswap! (dk (:id e)) into e)
        :else                 (insert! dk e))
  dk)

(defn !
  ([dk id k]
     (let [t (select dk id)]
       (t k)))
  ([dk id k v]
     (update! dk {:id id k v})))

(defn init!
  ([dk v] (init! dk identity v))
  ([dk f v] (init! dk f v [:id]))
  ([dk f v ks]
     (empty! dk)
     (.setRequired dk ks)
     (doseq [e v] (update! dk (f e)))
     dk))

;; Generalised Data Methods
(defn- *op [func e & xs]
  {:post [(= (:id e) (:id %))]}
  (apply (partial func e) xs))

(defn- contains-all? [m ks]
  (every? #(contains? m %) ks))

(defn op! [dk id func & args]
  {:pre  [(has-id? dk id)]
   :post [(contains-all? (select dk id) (.getRequired dk))]}
  (let [ae (dk id)]
    (iswap! ae
           (fn [_] (apply *op func @ae args))))
  dk)

(defn op-pred! [dk pred func & args]
  (let [pids (map :id (search dk pred))]
    (doseq [id pids]
      (apply (partial op! dk id func) args)))
  dk)

(defn op-all! [dk func & args]
  (doseq [id (ids dk)]
    (apply (partial op! dk id func) args))
  dk)

(defn assoc-in! [dk id & args]
  (apply op! dk id assoc args))

(defn dissoc-in! [dk id & args]
  (apply op! dk id dissoc args))

(defn update-in! [dk id ks func]
  (apply op! dk id update-in [ks func]))

(defn reset-in! [dk id val]
  {:pre  [(has-id? dk id)]
   :post [(contains-all? (select dk id) (.getRequired dk))
          (= id (:id (select dk id)))]}
  (ireset! (dk id) val))

(defn save-deck [dk f]
  (spit f (select dk)))

#_(defn load-deck [f]
  (init!
   (read-string (apply str (slurp f)))))
