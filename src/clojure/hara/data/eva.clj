(ns hara.data.eva
  (:refer-clojure :exclude [swap! reset!])
  (:use [hara.fn :only [deref*]]
        [hara.data.evom :only [evom swap! reset! add-watches remove-watches]])
  (:require [clojure.string :as s])
  (:gen-class
   :name hara.data.Eva
   :prefix "-"
   :init init
   :constructors {[] []}
   :state state
   :extends clojure.lang.AFn
   :implements [clojure.lang.IRef
                clojure.lang.Seqable
                clojure.lang.ILookup
                clojure.lang.ITransientVector]
   :methods [[setElemValidator [clojure.lang.IFn] void]
             [getElemValidator [] clojure.lang.IFn]
             [addElemWatch [java.lang.Object clojure.lang.IFn] void]
             [removeElemWatch [java.lang.Object] void]
             [getElemWatches [] clojure.lang.IPersistentMap]]))

(defn- sel [this] (:data (.state this)))

(defn -init
  ([]  [[]  {:data      (evom [])
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

(defn -setElemValidator [this vf]
  (doseq [entry (seq this)]
    (.setValidator (second entry) vf)))

(defn -getElemValidator [this]
  (if-let [l (seq this)]
    (.getValidator (-> l first second))))

(defn -getElemWatches [this]
  @(:watches (.state this)))

(defn -addElemWatch [this k f]
  (swap! (:watches (.state this)) assoc k f)
  (doseq [entry (seq this)]
    (add-watch entry k f)))

(defn -removeElemWatch [this k]
  (swap! (:watches (.state this)) dissoc k)
  (doseq [entry (seq this)]
    (remove-watch entry k)))

(defn- -toString [this]
  (->> @this (map #(-> % deref str)) (s/join "\n")))

(defn -deref [this] @(sel this))

(defn -valAt
  ([this k] (get (-deref this) k))
  ([this k nv] (get (-deref this) k nv)))

(defn -seq [this] (seq (-deref this)))

(defn -count [this] (count (-deref this)))

(defn -nth
  ([this i] (-valAt this i))
  ([this i nv] (-valAt this i nv)))

(defn -invoke
  ([this k] (-valAt this k))
  ([this k nv] -valAt this k nv))

(defn -conj [this v]
  (let [ev (evom v)]
    (add-watches ev (-getElemWatches this))
    (swap! (sel this) conj ev)
    this))

(defn -assoc [this k v]
  (if-let [pv (-valAt this k)]
    (remove-watches pv (keys (-getElemWatches this))))
  (let [ev (evom v)]
    (add-watches ev (-getElemWatches this))
    (swap! (sel this) assoc k ev)
    this))

(defn -assocN [this i v]
  (-assoc this i v))

(defn -pop [this]
  (if-let [lv (last this)]
    (remove-watches lv (keys (-getElemWatches this))))
  (swap! (sel this) pop)
  this)

(defn -persistent [this]
  (deref* (sel this)))

(defmethod print-method
  hara.data.Eva
  [this w]
  (print-method
   (let [hash (.hashCode this)]
     (->>  @this (map #(-> % deref))
           (cons (symbol (str "Eva@" hash)))
           vec)) w))


(comment 'a 'b)
;; (def a (-init ))
;; (sel a)
;; (-conj a 4)
