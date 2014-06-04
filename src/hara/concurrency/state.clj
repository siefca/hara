(ns hara.concurrency.state
  (:require []
            [hara.expression.shorthand :refer [get-> eq->]]
            [hara.common.checks :refer [atom? aref?]]))

;; ## IRef Functions

(defn hash-keyword
  "Returns a keyword repesentation of the hash-code.
   For use in generating internally unique keys

    (h/hash-keyword 1)
    ;=> :__1__
  "
  [obj & ids]
  (keyword (str "__" (st/join "_" (concat (map str ids) [(.hashCode obj)])) "__")))

(defn hash-pair
  "Combines the hash of two objects together.

    (hash-pair 1 :1)
    ;=> :__1_1013907437__
  "
  [v1 v2]
  (hash-keyword v2 (.hashCode v1)))

(defn set-value!
  "Change the value contained within a ref or atom.

    @(set-value! (atom 0) 1)
    ;=> 1

    @(set-value! (ref 0) 1)
    ;=> 1
  "
  [rf obj]
  (cond (atom? rf) (reset! rf obj)
        (aref? rf) (dosync (ref-set rf obj)))
  rf)

(defn alter!
  "Updates the value contained within a ref or atom using `f`.

    @(alter! (atom 0) inc)
    ;=> 1

    @(alter! (ref 0) inc)
    ;=> 1
  "
  [rf f & args]
  (cond (atom? rf) (apply swap! rf f args)
        (aref? rf) (dosync (apply alter rf f args)))
  rf)

(defn dispatch!
  "Updates the value contained within a ref or atom using another thread.

    (dispatch! (atom 0)
                (fn [x] (Thread/sleep 1000)
                        (inc x)))
    ;=> <future_call>
  "
  [ref f & args]
  (future
    (apply alter! ref f args)))

(declare add-change-watch
         make-change-watch)

(defn add-change-watch
  "Adds a watch function that only triggers when there is change
   in `(sel <value>)`.

    (def subject (atom {:a 1 :b 2}))
    (def observer (atom nil)
    (add-change-watch subject :clone
        :b (fn [& _] (reset! observer @a)))

    (swap! subject assoc :a 0)
    @observer => nil

    (swap! subject assoc :b 1)
    @observer => {:a 0 :b 1}
  "
  ([rf k f] (add-change-watch rf k identity f))
  ([rf k sel f]
     (add-watch rf k (make-change-watch sel f))))

(defn make-change-watch
  [sel f]
  (fn [k rf p n]
    (let [pv (get-> p sel)
          nv (get-> n sel)]
      (if-not (or (= pv nv) (nil? nv))
        (f k rf pv nv)))))