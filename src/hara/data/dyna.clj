(ns hara.data.dyna
  (:import hara.data.DynaRec))

(defn new
  ([] (DynaRec.))
  ([v & [ks]]
     (let [ks (or ks (-> v first keys))
           da (DynaRec. ks)]
       (dosync
        (doseq [e v]
          (conj! da e)))
       da)))

(defn- $ [da] (:data (.state da)))

(defn ids [da] (keys da))

(defn has-id? [da id] (contains? @da id))

(defn ch-id! [da oid nid]
  {:pre [(has-id? da oid)
         (or (not (has-id? da nid))
             (= oid nid))]}
  (let [ne  (assoc @(da oid) :id nid)]
    (dosync
     (dissoc! da oid)
     (conj! da ne))))

(defn search
  ([da]
     (search da (fn [_] true)))
  ([da pred]
     (search da
             pred
             (fn [x y] (.compareTo (:id x) (:id y)))))
  ([da pred comp]
     (let [pred (if (fn? pred)
                  pred
                  #(= pred (:id %)))]
       (->> (ids da)
            (map #(da %))
            (map deref)
            (filter pred)
            (sort comp)))))

(defn select
  [da id]
  {:pre [(has-id? da id)]}
  @(da id))

(defn empty! [da]
  (dosync (alter ($ da) empty) da))

(defn delete! [da id]
  {:pre [(has-id? da id)]}
  (dosync (dissoc! da id)))

(defn insert! [da e]
  {:pre [(not (has-id? da (:id e)))]}
  (dosync (conj! da e)))

(defn update! [da e]
  (cond (has-id? da (:id e))  (swap! (da (:id e)) into e)
        :else                 (insert! da e))
  da)

(defn !
  ([da id k]
     (let [t (select da id)]
       (t k)))
  ([da id k v]
     (update! da {:id id k v})))

(defn init!
  ([da v] (init! da identity v))
  ([da f v] (init! da f v [:id]))
  ([da f v ks]
     (empty! da)
     (.setRequired da ks)
     (doseq [e v] (update! da (f e)))
     da))

;; Generalised Data Methods
(defn- *op [func e & xs]
  {:post [(= (:id e) (:id %))]}
  (apply (partial func e) xs))

(defn- contains-all? [m ks]
  (every? #(contains? m %) ks))

(defn op! [da id func & args]
  {:pre  [(has-id? da id)]
   :post [(contains-all? (select da id) (.getRequired da))]}
  (let [ae (da id)]
    (swap! ae
           (fn [_] (apply *op func @ae args))))
  da)

(defn op-pred! [da pred func & args]
  (let [pids (map :id (search da pred))]
    (doseq [id pids]
      (apply (partial op! da id func) args)))
  da)

(defn op-all! [da func & args]
  (doseq [id (ids da)]
    (apply (partial op! da id func) args))
  da)

(defn assoc-in! [da id & args]
  (apply op! da id assoc args))

(defn dissoc-in! [da id & args]
  (apply op! da id dissoc args))

(defn update-in! [da id ks func]
  (apply op! da id update-in [ks func]))

(defn save-deck [dk f]
  (spit f (select dk)))

#_(defn load-deck [f]
  (init!
   (read-string (apply str (slurp f)))))
