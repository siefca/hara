(ns hara.ova
  (:use [hara.ova.impl :only [state add-iwatch remove-iwatch]]
        [hara.common :only [get-sel suppress eq-prchk suppress-prchk]])
  (:require [clojure.set :as set])
  (:import hara.ova.Ova))


(defn concat! [ova es]
  (doseq [e es] (conj! ova e))
  ova)

(defn append! [ova & es] (concat! ova es))

(defn empty! [ova] (.empty ova))

(defn refresh!
  ([ova coll]
     (empty! ova)
     (concat! ova coll)))

(defn reinit!
  ([ova]
     (.reset ova))
  ([ova coll]
     (.reset ova)
     (concat! ova coll)
     ova))

(defn ova
  ([] (Ova.))
  ([coll]
     (let [ova (Ova.)]
       (dosync (concat! ova coll))
       ova)))

(defn clear-watches [ova]
  (.clearWatches ova))

(defn add-elem-watch [ova k f]
  (.addElemWatch ova k f))

(defn remove-elem-watch [ova k]
  (.removeElemWatch ova k))

(defn get-elem-watches [ova]
  (.getElemWatches ova))

(defn clear-elem-watches [ova]
  (.clearElemWatches ova))

(defn make-elem-change-watch [ks f]
    (fn [k ov rf p n]
      (let [pv (get-in p ks)
            nv (get-in n ks)]
        (if (not= pv nv)
          (f k ov rf pv nv)))))

(defn make-elem-change-watch [sel f]
  (fn [k ov rf p n]
    (let [pv (get-sel p sel)
          nv (get-sel n sel)]
      (if-not (and (nil? pv) (nil? nv)
                   (= pv nv))
        (f k ov rf pv nv)))))

(defn add-elem-change-watch [ov k sel f]
  (add-elem-watch ov k (make-elem-change-watch sel f)))


(defn indices
  [ova prchk]
  (cond
   (number? prchk)
   (if (suppress (get ova prchk)) #{prchk} #{})

   (set? prchk)
   (set (mapcat #(indices ova %) prchk))

   :else
   (set (filter (comp not nil?)
                (map-indexed (fn [i obj]
                               (suppress-prchk obj prchk i))
                             ova)))))

(defn select
  [ova prchk]
  (cond
   (number? prchk)
   (if-let [val (suppress (get ova prchk))]
     #{val} #{})

   (set? prchk)
   (set (mapcat #(select ova %) prchk))

   :else
   (set (filter (fn [obj] (suppress-prchk obj prchk obj)) ova))))

(defn map! [ova f & args]
  (doseq [evm @ova]
    (apply alter evm f args))
  ova)

(defn map-indexed! [ova f]
  (doseq [i (range (count ova))]
    (alter (@ova i) #(f i %) ))
  ova)

(defn smap! [ova prchk f & args]
  (let [idx (indices ova prchk)]
    (doseq [i idx]
      (apply alter (@ova i) f args)))
  ova)

(defn smap-indexed! [ova prchk f]
  (let [idx (indices ova prchk)]
    (doseq [i idx]
      (alter (@ova i) #(f i %))))
  ova)

(defn insert-fn [v val & [i]]
  (if (nil? i)
    (conj v val)
    (vec (clojure.core/concat (conj (subvec v 0 i) val)
                              (subvec v i)))))

(defn insert! [ova val & [i]]
  (let [evm (ref val)]
    (add-iwatch ova evm)
    (alter (state ova) insert-fn evm i))
  ova)

(defn sort! [ova comp]
  (alter (state ova)
         #(sort (fn [x y]
                  ((or comp compare) @x @y)) %))
  ova)

(defn reverse! [ova]
  (alter (state ova) reverse)
  ova)

(defn- delete-iwatches [ova idx]
  (map-indexed (fn [i obj]
                 (if-not (idx i)
                   obj
                   (do (remove-iwatch ova obj) obj)))
               @ova)
  ova)

(defn- delete-iobjs [ova indices]
  (->> ova
       (map-indexed (fn [i obj] (if-not (indices i) obj)))
       (filter (comp not nil?))
       vec))

(defn delete-indices [ova idx]
  (delete-iwatches ova idx)
  (alter (state ova) delete-iobjs idx)
  ova)

(defn remove! [ova prchk]
  (let [idx (indices ova prchk)]
    (delete-indices ova idx))
  ova)

(defn filter! [ova prchk]
  (let [idx (set/difference
             (set (range (count ova)))
             (indices ova prchk))]
    (delete-indices ova idx))
  ova)


(comment

  (defn vset! [ova chk val]
    (smap! ova chk (constantly val)))

  (defn vinto! [ova chk val]
    (smap! ova chk #(into % val)))

  (defn vassoc! [ova chk & kvs]
    (smap! ova chk #(apply assoc % kvs)))

  (defn vassoc-in! [ova chk ks v]
    (smap! ova chk #(assoc-in % ks v)))

  (defn vdissoc! [ova chk & ks]
    (smap! ova chk #(apply dissoc % ks)))

  (defn vupdate-in! [ova chk ks f]
    (smap! ova chk #(update-in % ks f))))
