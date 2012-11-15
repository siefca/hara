(ns hara.ova
  (:use [hara.data.ova :only [sel add-iwatch del-iwatch]]
        [hara.fn :only [look-up]])
  (:import hara.data.Ova))

(defn ova
  ([] (Ova.))
  ([v]
     (let [ova (Ova.)]
       (dosync
        (doseq [e v]
          (conj! ova e)))
       ova)))

(defn add-elem-watch [^hara.data.Ova ova k f]
  (.addElemWatch ova k f))

(defn remove-elem-watch [^hara.data.Ova ova k]
  (.removeElemWatch ova k))

(defn get-elem-watches [^hara.data.Ova ova]
  (.getElemWatches ova))

(defn set-elem-validator [^hara.data.Ova ova v]
  (.setElemValidator ova v))

(defn get-elem-validator [^hara.data.Ova ova]
  (.getElemValidator ova))

(defn- try-chk [chk val res] ;; returns `res` if `(chk val)` succeeds, otherwise returns nil. Will swallow all thrown exceptions.
  (try (if (chk val) res)
       (catch Throwable t)))

(defn- get-val [obj k] ;; if k is a vector, then do a lookup of obj using keys contained in k, else do a normal lookup
  (if (vector? k)
    (look-up obj k)
    (get obj k)))

(defn- match? [obj k chk]
  (cond
    (fn? chk)
    (try (chk (get-val obj k))
         (catch Throwable t))

    :else
    (= (get-val obj k) chk)))

(defn- all-match? [obj chk]
  (let [m (apply hash-map chk)]
    (every? #(apply match? obj %) m)))


(defn indices [ova chk]
  (cond
    (number? chk)
    (if (ova chk) [chk] [])

    (set? chk)
    (mapcat #(indices ova %) chk)

    (fn? chk)
    (filter (comp not nil?)
            (map-indexed (fn [i obj] (try-chk chk obj i))
                         ova))

    (vector? chk)
    (filter (comp not nil?)
            (map-indexed (fn [i obj] (if (all-match? obj chk) i))
                         ova))))

(defn select [ova & [chk]]
  (cond
    (nil? chk)
    (map deref @(sel ova))

    (number? chk)
    (if-let [val (ova chk)]
      [val] [])

    (vector? chk)
    (filter #(all-match? % chk) ova)

    (set? chk)
    (map val (sort (select-keys (vec (seq ova)) chk)))

    (fn? chk)
    (filter (fn [obj] (try-chk chk obj true)) ova)))

(defn map! [ova f & args]
  (doseq [evm @ova]
    (apply alter evm f args))
  ova)

(defn map-indexed! [ova f & args]
  (doseq [i  (range (count ova))]
    (alter (nth @ova i) (fn [x]
                          (apply f i x args)) ))
  ova)

(defn smap! [ova chk f & args]
  (cond
    (number? chk)
    (if-let [evm (@ova chk)]
      (apply alter evm f args))

    (vector? chk)
    (map! ova (fn [obj]
                (if (all-match? obj chk)
                  (apply f obj args) obj)))

    (set? chk)
    (dorun (map-indexed (fn [i obj]
                          (if (chk i)
                            (apply alter obj f args)))
                        @ova))

    (fn? chk)
    (map! ova (fn [obj]
                (if (try-chk chk obj true)
                  (apply f obj args) obj))))
  ova)

(defn update! [ova chk val]
  (smap! ova chk #(into % val)))

(defn replace! [ova chk val]
  (smap! ova chk (constantly val)))

(defn update-in! [ova chk ks f]
  (smap! ova chk #(update-in % ks f)))

(defn replace-in! [ova chk ks val]
  (smap! ova chk #(update-in % ks (constantly val)) ))

(defn- -delete [v indices ova]
  (let [sdx (apply hash-set indices)]
    (->> v
         (map-indexed (fn [i obj]
                        (if-not (sdx i)
                          obj
                          (do (del-iwatch ova obj) nil))))
         (filter (comp not nil?))
         vec)))

(defn delete! [ova chk]
  (let [idx (indices ova chk)]
    (alter (sel ova) -delete idx ova))
  ova)

(defn- -insert [v val & [i]]
  (if (nil? i)
    (conj v val)
    (vec (concat (conj (subvec v 0 i) val)
                 (subvec v i)))))

(defn insert! [ova val & [i]]
  (let [evm (ref val)]
    (add-iwatch ova evm)
    (alter (sel ova) -insert evm i))
  ova)

(defn sort! [ova & comp]
  (let [s-fn (fn [x y] ((or comp compare) @x @y))]
    (alter (sel ova) (fn [evm] (sort s-fn evm))))
  ova)

(defn filter! [ova pred]
  (let [ft-fn (fn [pred]
                (fn [x] (true? (pred @x))))]
    (alter (sel ova) #(filter (ft-fn pred) %)))
  ova)

(defn reverse! [ova]
  (alter (sel ova) reverse)
  ova)

(defn concat! [ova es]
  (doseq [e es]
    (insert! ova e))
  ova)
