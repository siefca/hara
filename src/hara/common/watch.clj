(ns hara.common.watch
  (:require [hara.protocol.watch :refer :all]
            [hara.expression.shorthand :refer [get->]]
            [hara.function.args :as args])
  (:refer-clojure :exclude [list remove set]))

(defn- wrap-select [f sel]
  (fn [& args]
    (let [[n p & more] (reverse args)
          pv    (get-> p sel)
          nv    (get-> n sel)]
      (apply f (->> more (cons pv) (cons nv) reverse)))))

(defn- wrap-diff [f]
  (fn [& args]
    (let [[nv pv & more] (reverse args)]
      (cond (and (nil? pv) (nil? nv))
            nil

            (or (nil? pv) (nil? nv)
                (not (= pv nv)))
            (apply f args)))))

(defn process-options
  [opts f]
  (let [_ (if (:args opts)
            (args/arg-check f (:args opts)))
        f (if (:diff opts)
            (wrap-diff f)
            f)
        f (if-let [sel (:select opts)]
            (wrap-select f sel)
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
    @observer => 1

    \"Alternatively, options can be given to either transform the current
    as well as to only execute the callback if there is a difference.\"

    (let [subject  (atom {:a 1 :b 2})
          observer (atom nil)]
    (watch/add subject :clone
               (fn [_ _ p n] (reset! observer n))
               {:select :b
                :diff true})

    (swap! subject assoc :a 0) ;; change in :a does not
    @observer => nil           ;; affect watch

    (swap! subject assoc :b 1) ;; change in :b does
    @observer => 1))"
  {:added "2.1"}
  ([obj f] (add obj nil f nil))
  ([obj k f] (add obj k f nil))
  ([obj k f opts]
     (-add-watch obj k f opts)))

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
  "Clears all watches form the object

  (let [subject   (atom nil)
        observer  (atom nil)]
    (watch/add subject :a (fn [_ _ _ n]))
    (watch/add subject :b (fn [_ _ _ n]))
    (watch/clear subject)
    (watch/list subject)) => {}"
  {:added "2.1"}
  ([obj] (clear obj nil))
  ([obj opts]
     (let [watches (list obj opts)]
       (doseq [k (keys watches)]
         (remove obj k opts)))))

(defn set
  "Sets a watch in the form of a map

  (let [obj   (atom nil)]
    (watch/set obj {:a (fn [_ _ _ n])
                    :b (fn [_ _ _ n])})
    (watch/list obj) => (contains {:a fn? :b fn?}))"
  {:added "2.1"}
  ([obj watches] (set obj watches nil))
  ([obj watches opts]
     (doseq [[k f] watches]
       (add obj k f opts))
     (list obj opts)))

(defn copy
  "Copies watches from one object to another

  (let [obj-a   (atom nil)
        obj-b   (atom nil)]
    (watch/set obj-a {:a (fn [_ _ _ n])
                      :b (fn [_ _ _ n])})
    (watch/copy obj-b obj-a)
    (watch/list obj-b) => (contains {:a fn? :b fn?}))"
  {:added "2.1"}
  ([to from] (copy to from nil))
  ([to from opts]
     (let [watches (list from opts)]
       (set to watches opts))))

(extend-protocol IWatch
  clojure.lang.IRef
  (-add-watch [obj k f opts]
    (add-watch obj k
               (process-options opts f))
    (.getWatches obj))

  (-list-watch [obj _]
    (.getWatches obj))

  (-remove-watch [obj k _]
    (remove-watch obj k)))
