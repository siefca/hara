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
    @observer => 1

    "Alternatively, options can be given to either transform the current
    as well as to only execute the callback if there is a difference."

    (let [subject  (atom {:a 1 :b 2})
          observer (atom nil)]
    (watch/add subject :clone
               (fn [_ _ p n] (reset! observer n))
               {:select :b
                :diff true})

    (swap! subject assoc :a 0) ;; change in :a does not
    @observer => nil           ;; affect watch

    (swap! subject assoc :b 1) ;; change in :b does
    @observer => 1)))



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

^{:refer hara.common.watch/clear :added "2.1"}
(fact "Clears all watches form the object"

  (let [subject   (atom nil)
        observer  (atom nil)]
    (watch/add subject :a (fn [_ _ _ n]))
    (watch/add subject :b (fn [_ _ _ n]))
    (watch/clear subject)
    (watch/list subject)) => {})

^{:refer hara.common.watch/set :added "2.1"}
(fact "Sets a watch in the form of a map"

  (let [obj   (atom nil)]
    (watch/set obj {:a (fn [_ _ _ n])
                    :b (fn [_ _ _ n])})
    (watch/list obj) => (contains {:a fn? :b fn?})))

^{:refer hara.common.watch/copy :added "2.1"}
(fact "Copies watches from one object to another"

  (let [obj-a   (atom nil)
        obj-b   (atom nil)]
    (watch/set obj-a {:a (fn [_ _ _ n])
                      :b (fn [_ _ _ n])})
    (watch/copy obj-b obj-a)
    (watch/list obj-b) => (contains {:a fn? :b fn?})))
