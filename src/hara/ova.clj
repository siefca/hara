(ns hara.ova
  (:use [hara.ova.impl :only [sel add-iwatch remove-iwatch]]
        [hara.common :only [suppress-pri eq-pri]])
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

(defn indices
  [ova pri]
  (cond
   (number? pri)
   (if (suppress (get ova pri)) #{pri} #{})

   (set? pri)
   (set (mapcat #(indices ova %) pri))

   :else
   (set (filter (comp not nil?)
                (map-indexed (fn [i obj]
                               (suppress-pri obj pri i))
                             ova)))))

(defn select
  [ova pri]
  (cond
   (number? pri)
   (if-let [val (suppress (get ova pri))]
     #{val} #{})

   (set? pri)
   (set (mapcat #(select ova %) pri))

   :else
   (set (filter (fn [obj] (suppress-pri obj pri obj)) ova))))

(defn map! [ova f & args]
  (doseq [evm @ova]
    (apply alter evm f args))
  ova)

(defn map-indexed! [ova f]
  (doseq [i (range (count ova))]
    (alter (@ova i) #(f i %) ))
  ova)

(defn smap! [ova pri f & args]
  (let [idx (indices ova pri)]
    (doseq [i idx]
      (apply alter (@ova i) f args)))
  ova)

(defn smap-indexed! [ova pri f]
  (let [idx (indices ova pri)]
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
    (alter (sel ova) insert-fn evm i))
  ova)

(defn sort! [ova comp]
  (alter (sel ova)
         #(sort (fn [x y]
                  ((or comp compare) @x @y)) %))
  ova)

(defn reverse! [ova]
  (alter (sel ova) clojure.core/reverse)
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

(defn remove! [ova pri]
  (let [idx (indices ova pri)]
    (delete-iwatches ova idx)
    (alter (sel ova) delete-iobjs idx))
  ova)

(defn filter! [ova pri]
  (let [idx (set/difference
             (set (range (count ova)))
             (indices ova pri))]
    (delete-iwatches ova idx)
    (alter (sel ova) delete-iobjs idx))
  ova)


(comment

  (defn set! [ova chk val]
    (smap! ova chk (constantly val)))



  (defn update! [ova chk val]
    (smap! ova chk #(into % val)))

  (defn assoc! [ova chk & kvs]
    (smap! ova chk #(apply clojure.core/assoc % kvs)))

  (defn dissoc! [ova chk & ks]
    (smap! ova chk #(apply clojure.core/dissoc % ks)))

  (defn update-in! [ova chk ks f]
    (smap! ova chk #(clojure.core/update-in % ks f)))

  (defn set-in! [ova chk ks val]
    (smap! ova chk #(clojure.core/update-in % ks (constantly val)) )))
