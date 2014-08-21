(ns hara.common.watch
  (:require [hara.protocol.watch :refer :all]
            [hara.expression.shorthand :refer [get->]]
            [hara.function.args :as args])
  (:refer-clojure :exclude [list remove]))

(defn wrap-select [f sel]
  (fn [& args]
    (let [[n p & more] (reverse args)
          pv    (get-> p sel)
          nv    (get-> n sel)]
      (apply f (-> more (cons p) (cons n) reverse)))))

(defn wrap-change? [f]
  (fn [& args]
    (let [[nv pv & more] (reverse args)]
      (cond (and (nil? pv) (nil? nv))
            nil

            (or (nil? pv) (nil? nv)
                (not (= pv nv)))
            (apply f args)))))

(defn process-options
  [opts f]
  (let [_ (args/arg-check f (or (:args opts) 4))
        f (if-let [sel (:select opts)]
            (wrap-select f sel)
            f)
        f (if (:change? opts)
            (wrap-change? f)
            f)]
    f))

(defn add
  "Adds a watch function through the IWatch protocol

  (let [subject (atom nil)
        observer  (atom nil)]
    (watch/add subject :follow
               (fn [_ _ _ n]
                 (reset! observer n)))
    (reset! subject 1)
    @observer => 1)"
  {:added "2.1"}
  ([obj f] (add obj nil f nil))
  ([obj k f] (add obj k f nil))
  ([obj k f opts]
     (let [f (process-watch-options opts f)]
       (-add-watch obj k f opts))))

(defn list
  "Lists watch functions through the IWatch protocol

  (let [subject   (atom nil)
        observer  (atom nil)]
    (watch/add subject :a (fn [_ _ _ n]))
    (watch/add subject :b (fn [_ _ _ n]))
    (watch/list subject) => (contains {:a fn? :b fn?}))"
  {:added "2.1"}
  ([obj] (list obj nil))
  ([obj opts] (-list-watch obj opts)))

(defn remove
  "Removes watch function through the IWatch protocol

  (let [subject   (atom nil)
        observer  (atom nil)]
    (watch/add subject :a (fn [_ _ _ n]))
    (watch/add subject :b (fn [_ _ _ n]))
    (watch/remove subject :b)
    (watch/list subject)) => (contains {:a fn?})"
  {:added "2.1"}
  ([obj]   (remove obj nil nil))
  ([obj k] (remove obj k nil))
  ([obj k opts] (-remove-watch obj k opts)))

(defn clear
  ([obj] (clear obj nil))
  ([obj opts]
     (let [watches (list obj opts)]
       (doseq [k (keys watches)]
         (remove obj k opts)))))

(extend-protocol IWatch
  clojure.lang.IRef
  (-add-watch [obj k f opts]
    (add-watch obj k f opts))

  (-list-watch [obj _]
    (.getWatches obj))

  (-remove-watch [obj k _]
    (remove-watch obj k)))
