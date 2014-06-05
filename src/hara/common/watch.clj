(ns hara.common.watch
  (:require [hara.protocol.watch :refer :all]
            [hara.expression.shorthand :refer [get->]])
  (:refer-clojure :exclude [list remove]))

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
  ([obj f] (-add-watch obj nil f))
  ([obj opts f]
     (-add-watch obj opts f)))

(defn list
  "Lists watch functions through the IWatch protocol

  (let [subject   (atom nil)
        observer  (atom nil)]
    (watch/add subject :a (fn [_ _ _ n]))
    (watch/add subject :b (fn [_ _ _ n]))
    (watch/list subject) => (contains {:a fn? :b fn?}))"
  {:added "2.1"}
  ([obj] (-list-watch obj nil))
  ([obj opts]
     (-list-watch obj opts)))

(defn remove
  "Removes watch function through the IWatch protocol

  (let [subject   (atom nil)
        observer  (atom nil)]
    (watch/add subject :a (fn [_ _ _ n]))
    (watch/add subject :b (fn [_ _ _ n]))
    (watch/remove subject :b)
    (watch/list subject)) => (contains {:a fn?})"
  {:added "2.1"}
  ([obj] (-remove-watch obj nil))
  ([obj opts]
     (-remove-watch obj opts)))

(defn add-change
  "Adds a watch function that only triggers when there is change
 in `(sel <value>)`.

  (let [subject  (atom {:a 1 :b 2})
        observer (atom nil)]
    (watch/add-change subject :clone
                      :b  (fn [_ _ _ n] (reset! observer n)))

    (swap! subject assoc :a 0) ;; change in :a does not
    @observer => nil           ;; affect watch

    (swap! subject assoc :b 1) ;; change in :b does
    @observer => 1)"
  {:added "2.1"}
  ([ref k f] (add-change ref k identity f))
  ([ref k sel f]
     (add ref k (fn [k ref p n]
                  (let [pv (get-> p sel)
                        nv (get-> n sel)]
                    (if-not (or (= pv nv) (nil? nv))
                      (f k ref pv nv)))))))

(extend-protocol IWatch
  clojure.lang.IRef
  (-add-watch [obj k f]
    (add-watch obj k f))

  (-list-watch [obj _]
    (.getWatches obj))

  (-remove-watch [obj k]
    (remove-watch obj k)))
