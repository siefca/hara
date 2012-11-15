(ns hara.data.ova
  (:use [hara.fn :only [deref*]])
  (:require [clojure.string :as s])
  (:gen-class
   :name hara.data.Ova
   :prefix "-"
   :init init
   :constructors {[] []}
   :state state
   :extends clojure.lang.AFn
   :implements [clojure.lang.IRef
                clojure.lang.Seqable
                clojure.lang.ILookup
                clojure.lang.ITransientVector]
   :methods [[addElemWatch [java.lang.Object clojure.lang.IFn] void]
             [removeElemWatch [java.lang.Object] void]
             [getElemWatches [] clojure.lang.IPersistentMap]]))

(defn -trigger-watch [eva ])

(defn make-keyword [this]
  (keyword (str "__" (.hashCode this) "__")))

(defn add-iwatch [this evm]
  (let [k  (make-keyword this)
        f  (fn [k & args]
             (doseq [w (deref (:watches (.state this)))]
               (let [wk (first w)
                     wf (second w)]
                 (apply wf wk this args))))]
    (add-watch evm k f)))

(defn del-iwatch [this evm]
  (let [k  (make-keyword this)]
    (remove-watch evm k)))


(defn sel [this] (:data (.state this)))

(defn -init
  ([]  [[]  {:data      (ref [])
             :watches   (atom {})}]))

(defn -setValidator [this vf]
  (.setValidator (sel this) vf))

(defn -getValidator [this]
  (.getValidator (sel this)))

(defn -getWatches [this]
  (.getWatches (sel this)))

(defn -addWatch [this k f]
  (add-watch (sel this) k f))

(defn -removeWatch [this k]
  (remove-watch (sel this) k))

(defn -getElemWatches [this]
  (deref (:watches (.state this))))

(defn -addElemWatch [this k f]
  (swap! (:watches (.state this)) assoc k f))

(defn -removeElemWatch [this k]
  (swap! (:watches (.state this)) dissoc k))

(defn- -toString [this]
  (->> @this (map #(-> % deref str)) (s/join "\n")))

(defn -deref [this] @(sel this))

(defn -valAt
  ([this k] (-valAt this k nil))
  ([this k nv]
     (if-let [evm (get (-deref this) k nil)]
       @evm nv)))

(defn -seq [this] (map deref (seq (-deref this))))

(defn -count [this] (count (-deref this)))

(defn -nth
  ([this i] (-valAt this i))
  ([this i nv] (-valAt this i nv)))

(defn -invoke
  ([this k] (-valAt this k))
  ([this k nv] -valAt this k nv))

(defn -conj [this v]
  (let [ev (ref v)]
    (add-iwatch this ev)
    (alter (sel this) conj ev)
    this))

(defn -assoc [this k v]
  (if-let [pv (get @this k)]
    (reset! pv v)
    (let [ev (ref v)]
      (add-iwatch this ev)
      (alter (sel this) assoc k ev)
      this)))

(defn -assocN [this i v]
  (-assoc this i v))

(defn -pop [this]
  (if-let [lv (last @this)]
    (del-iwatch this lv))
  (alter (sel this) pop)
  this)

(defn -persistent [this]
  (deref* (sel this)))

(defmethod print-method
  hara.data.Ova
  [this w]
  (print-method
   (let [hash (.hashCode this)]
     (->>  @this (map #(-> % deref))
           (cons (symbol (str "Ova@" hash)))
           vec)) w))
