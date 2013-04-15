(ns hara.ova
  (:refer-clojure :exclude [map map-indexed update-in sort filter reverse concat dissoc])
  (:use [hara.ova.ova :only [sel add-iwatch del-iwatch]]
        [hara.common :only [suppress cmp-chk-all]])
  (:import hara.ova.Ova))

(defn ova
  ([] (Ova.))
  ([v]
     (let [ova (Ova.)]
       (dosync
        (doseq [e v]
          (conj! ova e)))
       ova)))

(defn add-elem-watch [^hara.ova.Ova ova k f]
  (.addElemWatch ova k f))

(defn remove-elem-watch [^hara.ova.Ova ova k]
  (.removeElemWatch ova k))

(defn get-elem-watches [^hara.ova.Ova ova]
  (.getElemWatches ova))

(defn set-elem-validator [^hara.ova.Ova ova v]
  (.setElemValidator ova v))

(defn get-elem-validator [^hara.ova.Ova ova]
  (.getElemValidator ova))

(defn- try-chk [chk val res] ;; returns `res` if `(chk val)` succeeds, otherwise returns nil. Will swallow all thrown exceptions.
(suppress (if (chk val) res)))

(defn indices [ova chk]
  (cond
    (number? chk)
    (if (ova chk) [chk] [])

    (set? chk)
    (mapcat #(indices ova %) chk)

    (fn? chk)
    (clojure.core/filter (comp not nil?)
            (clojure.core/map-indexed (fn [i obj] (try-chk chk obj i))
                         ova))

    (vector? chk)
    (clojure.core/filter (comp not nil?)
            (clojure.core/map-indexed (fn [i obj] (if (cmp-chk-all obj chk) i))
                         ova))))

(defn select [ova & [chk]]
  (cond
    (nil? chk)
    (clojure.core/map deref @(sel ova))

    (number? chk)
    (if-let [val (ova chk)]
      [val] [])

    (vector? chk)
    (clojure.core/filter #(cmp-chk-all % chk) ova)

    (set? chk)
    (clojure.core/map val (clojure.core/sort (select-keys (vec (seq ova)) chk)))

    (fn? chk)
    (clojure.core/filter (fn [obj] (try-chk chk obj true)) ova)))

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
               (if (cmp-chk-all obj chk)
                 (apply f obj args) obj)))

    (set? chk)
    (dorun (clojure.core/map-indexed (fn [i obj]
                          (if (chk i)
                            (apply alter obj f args)))
                        @ova))

    (fn? chk)
    (map! ova (fn [obj]
                (if (try-chk chk obj true)
                  (apply f obj args) obj))))
  ova)
  
(defn set! [ova chk val]
  (smap! ova chk (constantly val)))

(defn- delete-fn [v indices ova]
(let [sdx (apply hash-set indices)]
  (->> v
       (clojure.core/map-indexed (fn [i obj]
                      (if-not (sdx i)
                        obj
                        (do (del-iwatch ova obj) nil))))
       (clojure.core/filter (comp not nil?))
       vec)))

(defn delete! [ova chk]
(let [idx (indices ova chk)]
  (alter (sel ova) delete-fn idx ova))
ova)

(defn insert-fn [v val & [i]]
(if (nil? i)
  (conj v val)
  (vec (clojure.core/concat (conj (subvec v 0 i) val)
               (subvec v i)))))

(defn insert! [ova val & [i]]
(let [evm (ref val)]
  (add-iwatch ova evm)
  (alter (sel ova) insert-fn evm i))
ova)

(defn sort! [ova & comp]
  (let [s-fn (fn [x y] ((or comp compare) @x @y))]
    (alter (sel ova) (fn [evm] (clojure.core/sort s-fn evm))))
  ova)

(defn filter! [ova pred]
  (let [ft-fn (fn [pred]
                (fn [x] (true? (pred @x))))]
    (alter (sel ova) #(clojure.core/filter (ft-fn pred) %)))
  ova)

(defn reverse! [ova]
  (alter (sel ova) clojure.core/reverse)
  ova)

(defn concat! [ova es]
  (doseq [e es]
    (insert ova e))
  ova)



(defn update! [ova chk val]
  (smap! ova chk #(into % val)))
  
(defn assoc! [ova chk & kvs]
  (smap! ova chk #(apply clojure.core/assoc % kvs)))

(defn dissoc! [ova chk & ks]
  (smap! ova chk #(apply clojure.core/dissoc % ks)))

(defn update-in! [ova chk ks f]
  (smap! ova chk #(clojure.core/update-in % ks f)))

(defn set-in! [ova chk ks val]
  (smap! ova chk #(clojure.core/update-in % ks (constantly val)) ))



