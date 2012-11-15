(ns hara.eva
  (:refer-clojure :exclude [swap! reset!])
  (:use [hara.data.evom :only [evom swap! reset!]]
        [hara.data.eva :only [sel add-iwatch del-iwatch]]
        [hara.fn :only [look-up]])
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


(defn indices [eva chk]
  (cond
    (number? chk)
    (if (eva chk) [chk] [])

    (set? chk)
    (mapcat #(indices eva %) chk)

    (fn? chk)
    (filter (comp not nil?)
            (map-indexed (fn [i obj] (try-chk chk obj i))
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
    (filter (fn [obj] (try-chk chk obj true)) eva)))

(defn map! [eva f & args]
  (doseq [evm @eva]
    (apply swap! evm f args))
  eva)

(defn map-indexed! [eva f & args]
  (doseq [i  (range (count eva))]
    (swap! (nth @eva i) (fn [x]
                          (apply f i x args)) ))
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
                (if (try-chk chk obj true)
                  (apply f obj args) obj))))
  eva)

(defn update! [eva chk val]
  (smap! eva chk #(into % val)))

(defn replace! [eva chk val]
  (smap! eva chk (constantly val)))

(defn update-in! [eva chk ks f]
  (smap! eva chk #(update-in % ks f)))

(defn replace-in! [eva chk ks val]
  (smap! eva chk #(update-in % ks (constantly val)) ))

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

(defn concat! [eva es]
  (doseq [e es]
    (insert! eva e))
  eva)
