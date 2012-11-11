(ns hara.eva
  (:refer-clojure :exclude [swap! reset!])
  (:use [hara.data.evom :only [evom swap! reset!]])
  (:import hara.data.Eva))

(defn eva
  ([] (Eva.))
  ([v]
     (let [eva (Eva.)]
       (dosync
        (doseq [e v]
          (conj! eva e)))
       eva)))

(defn- sel [^hara.data.Eva eva] (:data (.state eva)))

(defn add-elem-watch [^hara.data.Eva eva k f]
  (.addElemWatch eva k f))

(defn remove-elem-watch [^hara.data.Eva eva k]
  (.removeElemWatch eva k))

(defn get-elem-watches [^hara.data.Eva eva]
  (.getElemWatches eva))

(defn set-elem-validator [^hara.data.Eva eva v]
  (.setElemValidator eva v))

(defn get-elem-validator [^hara.data.Eva eva]
  (.getElemValidator eva))

(defn- match? [val key chk]
  (if (fn? chk)
    (chk (val key))
    (= (val key) chk)))

(defn- all-match? [val chk]
  (let [m (apply hash-map chk)]
    (every? #(apply match? val %) m)))

(defn- rm-indices [v indices]
  (let [sdx (apply hash-set indices)]
    (->> v
         (map-indexed (fn [i obj] (if-not (sdx i) obj)))
         (filter (comp not nil?))
         vec)))

(defn indices [eva chk]
  (cond
    (number? chk)
    (if (eva chk) [chk] [])

    (set? chk)
    (mapcat #(indices eva %) chk)

    (fn? chk)
    (filter (comp not nil?)
            (map-indexed (fn [i obj] (if (chk obj) i))
                         eva))

    (vector? chk)
    (filter (comp not nil?)
            (map-indexed (fn [i obj] (if (all-match? obj chk) i))
                         eva))))

(defn select [eva & [chk]]
  (cond
    (nil? chk)
    (persistent! eva)

    (number? chk)
    (if-let [val (eva chk)]
      [val] [])

    (vector? chk)
    (filter #(all-match? % chk) eva)

    (set? chk)
    (map val (sort (select-keys (vec (seq eva)) chk)))

    (fn? chk)
    (filter chk eva)))

(defn map! [eva f]
  (doseq [evm @eva]
    (swap! evm f))
  eva)

(defn smap! [eva chk f]
  (cond
    (number? chk)
    (if-let [evm (@eva chk)]
      (swap! evm f))

    (vector? chk)
    (map! eva (fn [obj]
                (if (all-match? obj chk)
                  (f obj) obj)))

    (set? chk)
    (dorun (map-indexed (fn [i obj]
                          (if (chk i)
                            (swap! obj f)))
                        @eva))

    (fn? chk)
    (map! eva (fn [obj]
                (if (chk obj) (f obj) obj))))
  eva)

(defn update! [eva chk val]
  (smap! eva chk #(into % val)))

(defn replace! [eva chk val]
  (smap! eva chk (constantly val)))

(defn delete! [eva chk]
  (let [ks  (keys (get-elem-watches eva))
        idx (indices eva chk)]
    (doseq [i idx
            k ks]
      (remove-watch (@eva i) k))
    (swap! (:data (.state eva)) rm-indices idx))
  eva)

(defn- -insert [v val & [i]]
  (if (nil? i)
    (conj v val)
    (vec (concat (conj (subvec v 0 i) val)
                 (subvec v i)))))

(defn insert! [eva val & [i]]
  (let [ws (get-elem-watches eva)]
    (swap! (:data (.state eva)) -insert (evom val) i)
    (doseq [w ws]
      (add-watch (@eva i) (first w) (second w))))
  eva)


(comment

  (defn search
    ([^hara.data.Eva eva]
       (search eva (fn [_] true)))
    ([^hara.data.Eva eva pred]
       (search eva
               pred
               (fn [x y] (.compareTo (:id x) (:id y)))))
    ([^hara.data.Eva eva pred comp]
       (let [pred (if (fn? pred)
                    pred
                    #(= pred (:id %)))]
         (->> (ids eva)
              (map #(eva %))
              (map deref)
              (filter pred)
              (sort comp)))))

  (defn select
    [^hara.data.Eva eva id]
    {:pre [(has-id? eva id)]}
    @(eva id))

  (defn empty! [^hara.data.Eva eva]
    (doseq [w (.getElemWatches eva)]
      (remove-elem-watch eva w))
    (dosync (alter (sel eva) empty) eva))

  (defn delete! [^hara.data.Eva eva id]
    (dosync (dissoc! eva id)))

  (defn insert! [^hara.data.Eva eva e]
    ;;{:pre [(not (has-id? eva (:id e)))]}
    (dosync (conj! eva e)))

  (defn update! [^hara.data.Eva eva e]
    (cond (has-id? eva (:id e))  (iswap! (eva (:id e)) into e)
          :else                 (insert! eva e))
    eva)

  (defn !
    ([^hara.data.Eva eva id k]
       (let [t (select eva id)]
         (t k)))
    ([^hara.data.Eva eva id k v]
       (update! eva {:id id k v})))

  (defn init!
    ([^hara.data.Eva eva v] (init! eva identity v))
    ([^hara.data.Eva eva f v] (init! eva f v [:id]))
    ([^hara.data.Eva eva f v ks]
       (empty! eva)
       (.setRequired eva ks)
       (doseq [e v] (update! eva (f e)))
       eva))

  ;; Generalised Data Methods
  (defn- *op [func e & xs]
    {:post [(= (:id e) (:id %))]}
    (apply (partial func e) xs))

  (defn- contains-all? [m ks]
    (every? #(contains? m %) ks))

  (defn op! [^hara.data.Eva eva id func & args]
    {:pre  [(has-id? eva id)]
     :post [(contains-all? (select eva id) (.getRequired eva))]}
    (let [ae (eva id)]
      (iswap! ae
              (fn [_] (apply *op func @ae args))))
    eva)

  (defn op-pred! [^hara.data.Eva eva pred func & args]
    (let [pids (map :id (search eva pred))]
      (doseq [id pids]
        (apply (partial op! eva id func) args)))
    eva)

  (defn op-all! [^hara.data.Eva eva func & args]
    (doseq [id (ids eva)]
      (apply (partial op! eva id func) args))
    eva)

  (defn assoc-in! [^hara.data.Eva eva id & args]
    (apply op! eva id assoc args))

  (defn dissoc-in! [^hara.data.Eva eva id & args]
    (apply op! eva id dissoc args))

  (defn update-in! [^hara.data.Eva eva id ks func]
    (apply op! eva id update-in [ks func]))

  (defn reset-in! [^hara.data.Eva eva id val]
    {;;;:pre  [(has-id? eva id)]
     :post [(contains-all? (select eva id) (.getRequired eva))
            (= id (:id (select eva id)))]}
    (ireset! (eva id) val))

  (defn save [^hara.data.Eva eva f]
    (spit f (select eva)))

  #_(defn load [f]
      (init!
       (read-string (apply str (slurp f))))))
