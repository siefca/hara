(ns hara.ova.impl
  (:use [hara.common :only [deref* sel-chk suppress hash-keyword]])
  (:require [clojure.string :as s])
  (:gen-class
   :name hara.ova.Ova
   :prefix "-"
   :init init
   :constructors {[] []}
   :state state
   :extends clojure.lang.AFn
   :implements [clojure.lang.IRef
                clojure.lang.Seqable
                clojure.lang.ILookup
                clojure.lang.ITransientVector]
   :methods [[empty [] hara.ova.Ova]
             [reset [] hara.ova.Ova]
             [clearWatches [] void]
             [addElemWatch [java.lang.Object clojure.lang.IFn] void]
             [removeElemWatch [java.lang.Object] void]
             [getElemWatches [] clojure.lang.IPersistentMap]
             [clearElemWatches [] void]]))

(defn add-iwatch [this irf]
  (let [k  (hash-keyword this)
        f  (fn [k & args]
             (doseq [w (deref (:watches (.state this)))]
               (let [wk (first w)
                     wf (second w)]
                 (apply wf wk this args))))]
    (add-watch irf k f)))

(defn remove-iwatch [this irf]
  (let [k  (hash-keyword this)]
    (remove-watch irf k)))

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

(defn -clearWatches [this]
  (doseq [[k _] (-getWatches this)]
    (remove-watch (sel this) k)))

(defn -getElemWatches [this]
  (deref (:watches (.state this))))

(defn -addElemWatch [this k f]
  (swap! (:watches (.state this)) assoc k f))

(defn -removeElemWatch [this k]
  (swap! (:watches (.state this)) dissoc k))

(defn -clearElemWatches [this]
  (reset! (:watches (.state this)) {}))

(defn -toString [this]
  (->> @this (map #(-> % deref str)) (s/join "\n")))

(defn -deref [this] @(sel this))

(defn -persistent [this]
  (deref* (sel this)))

(defn -seq [this]
  (let [res (map deref (seq (-deref this)))]
    (if-not (empty? res) res)))

(defn -count [this] (count (-deref this)))

(defn -nth
  ([this i] (-nth this i nil))
  ([this i nv]
     (if-let [evm (nth (-deref this) i)]
       @evm nv)))

(defn -valAt
  ([this k] (-valAt this k nil nil))
  ([this k nv] (-valAt this k nil nv))
  ([this k sel nv]
    (cond
     (and (nil? sel) (integer? k))
     (-nth this k nv)

     :else
     (let [res (->> (map deref (-deref this))
                    (filter (fn [m] (sel-chk m (or sel :id) k)))
                    first)]
       (or res nv)))))

(defn -invoke
  ([this k] (-valAt this k))
  ([this k nv] (-valAt this k nv))
  ([this k sel nv] (-valAt this k sel nv)))

(defn -conj [this v]
  (let [ev (ref v)]
    (add-iwatch this ev)
    (alter (sel this) conj ev))
  this)

(defn -assoc [this k v]
  (if-let [pv (get (-deref this) k)]
    (ref-set pv v)
    (let [ev (ref v)]
      (add-iwatch this ev)
      (alter (sel this) assoc k ev)))
  this)

(defn -assocN [this i v]
  (-assoc this i v))

(defn -pop [this]
  (if-let [lv (last @this)]
    (remove-iwatch this lv))
  (alter (sel this) pop)
  this)

(defn -empty [this]
  (for [rf (-deref this)]
    (remove-iwatch this rf))
  (ref-set (sel this) [])
  this)

(defn -reset [this]
  (-empty this)
  (-clearWatches this)
  (-clearElemWatches this)
  this)

(defmethod print-method
  hara.ova.Ova
  [this w]
  (print-method
   (let [hash (.hashCode this)]
     (->>  @this (map #(-> % deref))
           (cons (symbol (str "Ova@" hash)))
           vec)) w))
