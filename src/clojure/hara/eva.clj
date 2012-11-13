(ns hara.eva
  (:refer-clojure :exclude [swap! reset!])
  (:use [hara.data.evom :only [evom swap! reset!]])
  (:use [hara.data.eva :only [sel add-iwatch del-iwatch]])
  (:import hara.data.Eva))

(defn eva
  ([] (Eva.))
  ([v]
     (let [eva (Eva.)]
       (dosync
        (doseq [e v]
          (conj! eva e)))
       eva)))


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

(defn map! [eva f & args]
  (doseq [evm @eva]
    (apply swap! evm f args))
  eva)

(defn smap! [eva chk f & args]
  (cond
    (number? chk)
    (if-let [evm (@eva chk)]
      (apply swap! evm f args))

    (vector? chk)
    (map! eva (fn [obj]
                (if (all-match? obj chk)
                  (apply f obj args) obj)))

    (set? chk)
    (dorun (map-indexed (fn [i obj]
                          (if (chk i)
                            (apply swap! obj f args)))
                        @eva))

    (fn? chk)
    (map! eva (fn [obj]
                (if (chk obj) (apply f obj args) obj))))
  eva)

(defn update! [eva chk val]
  (smap! eva chk #(into % val)))

(defn replace! [eva chk val]
  (smap! eva chk (constantly val)))

(defn- -delete [v indices eva]
  (let [sdx (apply hash-set indices)]
    (->> v
         (map-indexed (fn [i obj]
                        (if-not (sdx i)
                          obj
                          (do (del-iwatch eva obj) nil))))
         (filter (comp not nil?))
         vec)))

(defn delete! [eva chk]
  (let [idx (indices eva chk)]
    (swap! (sel eva) -delete idx eva))
  eva)

(defn- -insert [v val & [i]]
  (if (nil? i)
    (conj v val)
    (vec (concat (conj (subvec v 0 i) val)
                 (subvec v i)))))

(defn insert! [eva val & [i]]
  (let [evm (evom val)]
    (add-iwatch eva evm)
    (swap! (sel eva) -insert evm i))
  eva)

(defn sort! [eva & comp]
  (let [s-fn (fn [x y] ((or comp compare) @x @y))]
    (swap! (sel eva) (fn [evm] (sort s-fn evm))))
  eva)

(defn filter! [eva pred]
  (let [ft-fn (fn [pred]
                (fn [x] (true? (pred @x))))]
    (swap! (sel eva) #(filter (ft-fn pred) %)))
  eva)

(defn reverse! [eva]
  (swap! (sel eva) reverse)
  eva)

(comment
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
