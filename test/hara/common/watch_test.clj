(ns hara.common.watch-test
  (:use midje.sweet)
  (:require [hara.common.watch :as watch])
  (:refer-clojure :exclude [get set]))

^{:refer hara.common.watch/add :added "2.1"}
(fact "Adds a watch function through the IWatch protocol"

  (let [subject (atom nil)
        observer  (atom nil)]
    (watch/add subject :follow
               (fn [_ _ _ n]
                 (reset! observer n)))
    (reset! subject 1)
    @observer => 1))

^{:refer hara.common.watch/list :added "2.1"}
(fact "Lists watch functions through the IWatch protocol"

  (let [subject   (atom nil)
        observer  (atom nil)]
    (watch/add subject :a (fn [_ _ _ n]))
    (watch/add subject :b (fn [_ _ _ n]))
    (watch/list subject) => (contains {:a fn? :b fn?})))

^{:refer hara.common.watch/remove :added "2.1"}
(fact "Removes watch function through the IWatch protocol"

  (let [subject   (atom nil)
        observer  (atom nil)]
    (watch/add subject :a (fn [_ _ _ n]))
    (watch/add subject :b (fn [_ _ _ n]))
    (watch/remove subject :b)
    (watch/list subject)) => (contains {:a fn?}))

^{:refer hara.common.watch/add-change :added "2.1"}
(fact "Adds a watch function that only triggers when there is change
 in `(sel <value>)`."

  (let [subject  (atom {:a 1 :b 2})
        observer (atom nil)]
    (watch/add-change subject :clone
                      :b  (fn [_ _ _ n] (reset! observer n)))

    (swap! subject assoc :a 0) ;; change in :a does not
    @observer => nil           ;; affect watch

    (swap! subject assoc :b 1) ;; change in :b does
    @observer => 1))
