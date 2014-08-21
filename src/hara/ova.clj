(ns hara.ova
  (:require [hara.common.state :as state]
            [hara.common.watch :as watch]
            [hara.common.error :refer [error]]
            [hara.common.hash :refer [hash-label]]
            [hara.expression.shorthand :refer [get-> check-> check?->]]
            [hara.function.args :as args]))

(defn- ova-state []
  {::data      (ref [])
   ::watches   (atom {})})

(defn- internal-watch-fn [ova]
  (fn [k & args]
    (doseq [w (hara.protocol.watch/-list-watch ova {:type :elements})]
      (let [wk (first w)
            wf (second w)]
        (apply wf wk ova args)))))

(defn- add-internal-watch [ova irf]
  (let [k  (-> ova hash-label keyword)
        f  (internal-watch-fn ova)]
    (add-watch irf k f)))

(defn- remove-internal-watch [ova irf]
  (let [k (-> ova hash-label keyword)]
    (remove-watch irf k)))

(defn- get-filtered [ova k sel nv]
  (cond (and (nil? sel) (integer? k))
        (nth ova k nv)

        :else
        (let [res (->> (map deref @ova)
                       (filter (fn [m] (check?-> m (or sel :id) k)))
                       first)]
          (or res nv))))

(defn- standardise-opts [opts]
  (cond (keyword? opts) {:type opts}
        :else opts))

(deftype Ova [state]

  hara.protocol.watch/IWatch
  (-add-watch    [obj k f opts]
    (let [opts (standardise-opts opts)]
      (cond (or (= (:type opts) :ova))
            (add-watch (::data state) k
                       (watch/process-options opts f))

            :else
            (swap! (::watches state) assoc k
                   (watch/process-options (assoc opts :args 5) f)))))

  (-remove-watch [ova k opts]
    (let [opts (standardise-opts opts)]
      (cond (= (:type opts) :ova)
            (remove-watch (::data state) k)

            :else
            (swap! (::watches state) dissoc k))))

  (-list-watch [obj opts]
    (let [opts (standardise-opts opts)]
      (cond (= (:type opts) :ova)
            (.getWatches (::data state))

            :else
            (deref (::watches state)))))

  hara.protocol.state/IStateGet
  (-get-state [ova opts]
    (deref [ova] @(::data state)))

  hara.protocol.state/IStateSet
  (-empty-state [ova opts]
    (doseq [rf @(::data state)]
      (remove-internal-watch ova rf))
    (ref-set (::data state) [])
    ova)

  (-set-state [ova opts arr]
    (state/empty ova)
    (doseq [e arr]
      (conj! ova e)))

  clojure.lang.IDeref
  (deref [ova] @(::data state))

  clojure.lang.IRef
  (setValidator [ova vf]
    (.setValidator (::data state) vf))

  (getValidator [ova]
    (.getValidator (::data state)))

  (getWatches [ova]
    (.getWatches (::data state)))

  (addWatch [ova key callback]
    (add-watch (::data state) key callback))

  (removeWatch [ova key]
    (remove-watch (::data state) key))

  clojure.lang.ITransientCollection
  (conj [ova v]
    (let [ev (ref v)]
      (add-internal-watch ova ev)
      (alter (::data state) conj ev))
    ova)

  (persistent [ova]
    (mapv deref @(::data state)))

  clojure.lang.ITransientAssociative
  (assoc [ova k v]
    (if-let [pv (get @ova k)]
      (ref-set pv v)
      (let [ev (ref v)]
        (add-internal-watch ova ev)
        (alter (::data state) assoc k ev)))
    ova)

  clojure.lang.ITransientVector
  (assocN [ova i v] (assoc ova i v))

  (pop [ova]
    (if-let [lv (last @ova)]
      (remove-internal-watch ova lv))
    (alter (::data state) pop)
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
  (invoke [ova sel k] (get-filtered ova k sel nil))

  java.lang.Object
  (toString [ova]
    (str "#ova " (persistent! ova))))

(defmethod print-method Ova
  [v w]
  (.write w (str v)))

(defn concat! [ova es]
  (doseq [e es] (conj! ova e))
  ova)

(defn append! [ova & es]
  (concat! ova es))

(defn init!
  ([ova]
     (watch/clear ova)
     (watch/clear ova :ova)
     (state/empty ova)
     ova)
  ([ova coll]
     (init! ova)
     (state/set ova coll)
     ova))

(defn ova
  ([] (Ova. (ova-state)))
  ([coll]
     (let [ova (Ova. (ova-state))]
       (dosync
        (state/set ova coll))
       ova)))




(dosync (-> (Ova. (ova-state))
            (init! [{:id 3}])
            ;;(concat! [{:id 3} {:id 4} {:id 5}])
            ;;(concat! [{:id 3} {:id 4} {:id 5}])
            ))

(persistent!
 (dosync (-> (Ova. (ova-state))
             (conj! {:id 1})
             (state/set [{:id 3} {:id 4} {:id 5}])

             (conj! {:id 2})
             (state/empty)
             (conj! {:id 3})
             )))
