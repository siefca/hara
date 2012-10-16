(ns hara.dyna
  (:use [hara.iotam :only [iswap! ireset!]])
  (:import hara.data.DynaRec))

(defn dyna
  ([] (DynaRec.))
  ([v & [ks]]
     (let [ks (or ks (-> v first keys))
           dyna (DynaRec. ks)]
       (dosync
        (doseq [e v]
          (conj! dyna e)))
       dyna)))

(defn- $ [^hara.data.DynaRec dyna] (:data (.state dyna)))

(defn ids [^hara.data.DynaRec dyna] (keys dyna))

(defn has-id? [^hara.data.DynaRec dyna id] (contains? @dyna id))

(defn ch-id! [^hara.data.DynaRec dyna oid nid]
  {:pre [(has-id? dyna oid)
         (or (not (has-id? dyna nid))
             (= oid nid))]}
  (let [ne  (assoc @(dyna oid) :id nid)]
    (dosync
     (dissoc! dyna oid)
     (conj! dyna ne))))

(defn add-elem-watch [^hara.data.DynaRec dyna k f]
  (.addElemWatch dyna k f))

(defn remove-elem-watch [^hara.data.DynaRec dyna k]
  (.removeElemWatch dyna k))

(defn get-elem-watches [^hara.data.DynaRec dyna]
  (.getElemWatches dyna))

(defn set-elem-validator [^hara.data.DynaRec dyna v]
  (.setElemValidator dyna v))

(defn get-elem-validator [^hara.data.DynaRec dyna]
  (.getElemValidator dyna))

(defn search
  ([^hara.data.DynaRec dyna]
     (search dyna (fn [_] true)))
  ([^hara.data.DynaRec dyna pred]
     (search dyna
             pred
             (fn [x y] (.compareTo (:id x) (:id y)))))
  ([^hara.data.DynaRec dyna pred comp]
     (let [pred (if (fn? pred)
                  pred
                  #(= pred (:id %)))]
       (->> (ids dyna)
            (map #(dyna %))
            (map deref)
            (filter pred)
            (sort comp)))))

(defn select
  [^hara.data.DynaRec dyna id]
  {:pre [(has-id? dyna id)]}
  @(dyna id))

(defn empty! [^hara.data.DynaRec dyna]
  (doseq [w (.getElemWatches dyna)]
    (remove-elem-watch dyna w))
  (dosync (alter ($ dyna) empty) dyna))

(defn delete! [^hara.data.DynaRec dyna id]
  {:pre [(has-id? dyna id)]}
  (dosync (dissoc! dyna id)))

(defn insert! [^hara.data.DynaRec dyna e]
  {:pre [(not (has-id? dyna (:id e)))]}
  (dosync (conj! dyna e)))

(defn update! [^hara.data.DynaRec dyna e]
  (cond (has-id? dyna (:id e))  (iswap! (dyna (:id e)) into e)
        :else                 (insert! dyna e))
  dyna)

(defn !
  ([^hara.data.DynaRec dyna id k]
     (let [t (select dyna id)]
       (t k)))
  ([^hara.data.DynaRec dyna id k v]
     (update! dyna {:id id k v})))

(defn init!
  ([^hara.data.DynaRec dyna v] (init! dyna identity v))
  ([^hara.data.DynaRec dyna f v] (init! dyna f v [:id]))
  ([^hara.data.DynaRec dyna f v ks]
     (empty! dyna)
     (.setRequired dyna ks)
     (doseq [e v] (update! dyna (f e)))
     dyna))

;; Generalised Data Methods
(defn- *op [func e & xs]
  {:post [(= (:id e) (:id %))]}
  (apply (partial func e) xs))

(defn- contains-all? [m ks]
  (every? #(contains? m %) ks))

(defn op! [^hara.data.DynaRec dyna id func & args]
  {:pre  [(has-id? dyna id)]
   :post [(contains-all? (select dyna id) (.getRequired dyna))]}
  (let [ae (dyna id)]
    (iswap! ae
           (fn [_] (apply *op func @ae args))))
  dyna)

(defn op-pred! [^hara.data.DynaRec dyna pred func & args]
  (let [pids (map :id (search dyna pred))]
    (doseq [id pids]
      (apply (partial op! dyna id func) args)))
  dyna)

(defn op-all! [^hara.data.DynaRec dyna func & args]
  (doseq [id (ids dyna)]
    (apply (partial op! dyna id func) args))
  dyna)

(defn assoc-in! [^hara.data.DynaRec dyna id & args]
  (apply op! dyna id assoc args))

(defn dissoc-in! [^hara.data.DynaRec dyna id & args]
  (apply op! dyna id dissoc args))

(defn update-in! [^hara.data.DynaRec dyna id ks func]
  (apply op! dyna id update-in [ks func]))

(defn reset-in! [^hara.data.DynaRec dyna id val]
  {:pre  [(has-id? dyna id)]
   :post [(contains-all? (select dyna id) (.getRequired dyna))
          (= id (:id (select dyna id)))]}
  (ireset! (dyna id) val))

(defn save-deck [^hara.data.DynaRec dyna f]
  (spit f (select dyna)))

#_(defn load-deck [f]
  (init!
   (read-string (apply str (slurp f)))))
