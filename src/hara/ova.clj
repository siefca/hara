;; ##

(ns hara.ova
  (:use [hara.common :only [deref-nested sel-chk suppress hash-keyword
                            get-sel eq-prchk suppress-prchk]])
  (:require [clojure.string :as s]
            [clojure.set :as set]))

(defn ova-state []
  {:data      (ref [])
   :watches   (atom {})})

(defprotocol OvaProtocol
  (empty! [ova])
  (get-ref [ova])
  (clear-watches [ova])
  (add-elem-watch [ova k f])
  (remove-elem-watch [ova k])
  (get-elem-watches [ova])
  (clear-elem-watches [ova])
  (get-filtered [ova k sel nv]))

(defn make-iwatch [ova]
  (fn [k & args]
    (doseq [w (get-elem-watches ova)]
      (let [wk (first w)
            wf (second w)]
        (apply wf wk ova args)))))

(defn add-iwatch [ova irf]
  (let [k  (hash-keyword ova)
        f  (make-iwatch ova)]
    (add-watch irf k f)))

(defn remove-iwatch [ova irf]
  (let [k (hash-keyword ova)]
    (remove-watch irf k)))

(deftype Ova [state]
  OvaProtocol
  (empty! [ova]
    (for [rf @ova]
      (remove-iwatch ova rf))
    (ref-set (:data state) [])
    ova)

  (get-ref [ova]
    (:data state))

  (clear-watches [ova]
    (doseq [[k _] (.getWatches ova)]
      (remove-watch ova k)))

  (add-elem-watch [ova k f]
    (swap! (:watches state) assoc k f))

  (remove-elem-watch [ova k]
    (swap! (:watches state) dissoc k))

  (get-elem-watches [ova]
    (deref (:watches state)))

  (clear-elem-watches [ova]
    (reset! (:watches state) {}))

  (get-filtered [ova k sel nv]
    (cond (and (nil? sel) (integer? k))
          (nth ova k nv)

          :else
          (let [res (->> (map deref @ova)
                         (filter (fn [m] (sel-chk m (or sel :id) k)))
                         first)]
            (or res nv))))

  clojure.lang.IDeref
  (deref [ova] @(:data state))

  clojure.lang.IRef
  (setValidator [ova vf]
    (.setValidator (:data state) vf))

  (getValidator [ova]
    (.getValidator (:data state)))

  (getWatches [ova]
    (.getWatches (:data state)))

  (addWatch [ova key callback]
    (add-watch (:data state) key callback))

  (removeWatch [ova key]
    (remove-watch (:data state) key))

  clojure.lang.ITransientCollection
  (conj [ova v]
    (let [ev (ref v)]
      (add-iwatch ova ev)
      (alter (:data state) conj ev))
    ova)

  (persistent [ova]
    (deref-nested (:data state)))

  clojure.lang.ITransientAssociative
  (assoc [ova k v]
    (if-let [pv (get @ova k)]
      (ref-set pv v)
      (let [ev (ref v)]
        (add-iwatch ova ev)
        (alter (:data state) assoc k ev)))
    ova)

  clojure.lang.ITransientVector
  (assocN [ova i v] (assoc ova i v))

  (pop [ova]
    (if-let [lv (last @ova)]
      (remove-iwatch ova lv))
    (alter (:data state) pop)
    ova)

  clojure.lang.ILookup
  (valAt [ova k]
    (get-filtered ova k nil nil))

  (valAt [ova k not-found]
    (get-filtered ova k nil not-found))

  clojure.lang.Indexed
  (nth [ova i]
    (nth ova i nil))

  (nth [ova i not-found]
     (if-let [entry (nth @ova i)]
       @entry not-found))

  clojure.lang.Counted
  (count [ova] (count @ova))

  clojure.lang.Seqable
  (seq [ova]
    (let [res (map deref (seq @ova))]
      (if-not (empty? res) res)))

  clojure.lang.IFn
  (invoke [ova k] (get ova k))
  (invoke [ova k not-found] (get ova k not-found))
  (invoke [ova k sel not-found] (get-filtered ova k sel not-found))

  java.lang.Object
  (toString [ova]
    (str (persistent! ova))))

(defmethod print-method
  Ova
  [ova w]
  (print-method
   (let [hash (.hashCode ova)
         contents (->>  @ova
                        (mapv #(-> % deref)))]
     (format "<Ova@%s %s>"
             hash contents)) w))

(defn concat! [ova es]
  (doseq [e es] (conj! ova e))
  ova)

(defn append! [ova & es] (concat! ova es))

(defn refresh!
  ([ova coll]
     (empty! ova)
     (concat! ova coll)))

(defn reinit!
  ([ova]
     (empty! ova)
     (clear-watches ova)
     (clear-elem-watches ova)
     ova)
  ([ova coll]
     (reinit! ova)
     (concat! ova coll)
     ova))

(defn ova
  ([] (Ova. (ova-state)))
  ([coll]
     (let [ova (Ova. (ova-state))]
       (dosync (concat! ova coll))
       ova)))


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
    (alter (get-ref ova) insert-fn evm i))
  ova)

(defn sort! [ova comp]
  (alter (get-ref ova)
         #(sort (fn [x y]
                  ((or comp compare) @x @y)) %))
  ova)

(defn reverse! [ova]
  (alter (get-ref ova) reverse)
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
  (alter (get-ref ova) delete-iobjs idx)
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

(defn smap>> [ova prchk form]
  (cond (list? form)
        (apply list smap! ova prchk form)
        :else
        (list smap! ova prchk form)))

(defmacro >>> [ova prchk & forms]
  (cons 'do
        (for [form forms]
          (smap>> ova prchk form))))

(comment
  (def ov (ova [{}]))

  (dosync (>>> ov 0
               (assoc-in [:a :b] 1)
               (update-in [:a :b] inc)
               (assoc :c 3)))

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


;;(dosync (reinit! (ova [1 2 3 4]) [2 3 4 5 6]))
