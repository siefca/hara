(ns hara.data.dyna-rec
  (:use [hara.fn :only [deref*]]
        [hara.data.iotam :only [iotam iswap! ireset!]])
  (:require [clojure.string :as s])
  (:gen-class
   :name hara.data.DynaRec
   :prefix "-"
   :init init
   :constructors {[] []
                  [clojure.lang.Sequential] []}
   :state state
   :extends clojure.lang.AFn
   :implements [clojure.lang.IRef
                clojure.lang.Seqable
                clojure.lang.ILookup
                clojure.lang.ITransientMap]
   :methods [[getRequired [] clojure.lang.Seqable]
             [setRequired [clojure.lang.Seqable] void]
             [setElemValidator [clojure.lang.IFn] void]
             [getElemValidator [] clojure.lang.IFn]
             [addElemWatch [java.lang.Object clojure.lang.IFn] void]
             [removeElemWatch [java.lang.Object] void]
             [getElemWatches [] clojure.lang.IPersistentMap]]))

(defn- $ [this] (:data (.state this)))

(defn- valid?
  ([e]
     (valid? e [:id]))
  ([e ks]
     (every? #(contains? e %) ks)))

(defn- internal-data-valid? [this]
  (every? #(valid? @% (.getRequired this)) (vals @this)))

(defn -getRequired [this] @(:required (.state this)))

(defn -setRequired [this ks]
  {:post [(internal-data-valid? this)]}
  (swap! (:required (.state this))
         (fn [_]  (into (apply hash-set (seq ks)) #{:id}))))

(defn -setValidator [this vf]
  (.setValidator ($ this) vf))

(defn -getValidator [this]
  (.getValidator ($ this)))

(defn -getWatches [this] (.getWatches ($ this)))

(defn -addWatch [this k f]
  (add-watch ($ this) k f)
  this)

(defn -removeWatch [this k]
  (remove-watch ($ this) k)
  this)

(defn -setElemValidator [this vf]
  (doseq [entry (seq this)]
    (.setValidator (second entry) vf)))

(defn -getElemValidator [this]
  (if-let [l (seq this)]
    (.getValidator (-> l first second))))

(defn -getElemWatches [this] @(:watches (.state this)))

(defn -addElemWatch [this k f]
  (swap! (:watches (.state this)) assoc k f)
  (doseq [entry (seq this)]
    (add-watch (second entry) k f)))

(defn -removeElemWatch [this k]
  (swap! (:watches (.state this)) dissoc k)
  (doseq [entry (seq this)]
    (remove-watch (second entry) k)))

(defn- -toString [this]
  (->>  (vals @this) (map #(-> % deref str)) (s/join "\n")))

(defn- satisfied?
  ([this e] (valid? e (-getRequired this))))

(defn -deref [this] @($ this))

(defn -valAt
  ([this k] ((-deref this) k))
  ([this k nv] ((-deref this) k nv)))

(defn -invoke
  ([this k] (-valAt this k))
  ([this k nv] -valAt this k nv))

(defn -seq [this] (seq (-deref this)))

(defn -count [this] (count (-deref this)))

(defn -without [this k]
  (if-let [av (-valAt this k)]
    (doseq [watch (-getElemWatches this)]
      (remove-watch av (first watch))))
  (alter ($ this) dissoc k)
  this)

(defn -assoc
  ([this obj] (-assoc this (:id obj) obj))
  ([this k v]
   {:pre ["has to have the contents key" (satisfied? this v)
          (= k (:id v))]}
   (let [av (iotam v)]
     (alter ($ this) assoc k av)
     (doseq [watch (-getElemWatches this)]
       (add-watch av (first watch) (second watch))))
    this))

(defn -conj [this obj]
  (-assoc this obj))

(defn -persistent [this]
  (deref* ($ this)))

(defn -init
  ([]
     [[]  {:required  (atom #{:id})
           :data      (ref {})
           :watches   (atom {})}])
  ([ks]
     [[]  {:required  (atom (into (apply hash-set (seq ks)) #{:id}))
           :data      (ref {})
           :watches   (atom {})}]))

(defmethod print-method
  hara.data.DynaRec
  [this, w]
  (print-method (->>  (vals @this) (map #(-> % deref))  (cons 'DynaRec) vec ) w))
