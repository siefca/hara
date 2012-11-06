(ns hara.test-iotam
  (:use midje.sweet
        hara.iotam)
  (:require [hara.fn :as f]))

(facts "call-if-not-nil only executes the function is v is not null"
  (f/call-if-not-nil inc nil) => nil
  (f/call-if-not-nil inc 1) => 2)

(facts "look-up returns the value of a map according to its keys"
  (f/look-up nil nil) => nil
  (f/look-up nil []) => nil
  (f/look-up {} []) => {}
  (f/look-up {} [:any]) => nil
  (f/look-up {:a 1} [:a]) => 1
  (f/look-up {:a {:b 1}} [:a]) => {:b 1}
  (f/look-up {:a {:b 1}} [:a :b]) => 1
  (f/look-up {:a {:b 1}} [:a :c]) => nil
  (f/look-up {:a {:b {:c 1}}} [:a :b :c]) => 1
  (f/look-up {:a {:b {:c 1}}} [:b :c]) => nil)

(facts)


(defn fact$ [n k]
  ;;(println n)
  (if (zero? n)
    (k 1)
    (fact$ (- n 1)
           (fn [x] (k (* n x))))))

(fact$ 5 (fn [x] x))
(fact$ 5 list)


(defn snoc [l x]
  (concat l (list x)))

(defn reverse* [x]
  (cond
    (nil? (seq x)) ()

    (vector? (first x))
    (snoc (reverse* (next x)) (reverse* (first x)))

    :else
    (snoc (reverse* (next x)) (first x))))

(reverse* [1 2 [3 4 [6 7 8] 9 10] 11 12])



(defn apply-cont [k v] (k v))

(defn snoc-cps [l x k]
  (apply-cont k (conj l x)))

(defn reverse*-cps [l k]
  (cond
    (nil? (seq l))
    (apply-cont k [])

    (vector? (first l))
    (reverse*-cps
     (rest l)
     (fn [new-tail]
       (reverse*-cps
        (first l)
        (fn [new-head]
          (snoc-cps new-tail new-head k)))))

    :else
    (reverse*-cps
     (rest l)
     (fn [new-tail]
       (snoc-cps new-tail (first l) k)))))

(reverse*-cps [1 2 [3 4 [6 7 8]]] identity)

(defn apply-cont [k v]
  (k v))

(defn snoc-cps [v x k]
  (apply-cont k (conj v x)))

(defn reverse*-cps [x k]
  (cond
   (nil? (seq x)) (apply-cont k [])
   (seq? (first x))
     (reverse*-cps (rest x)
       (fn [new-tail]
         (reverse*-cps (first x)
           (fn [new-head]
             (snoc-cps new-tail new-head k)))))
   :else (reverse*-cps (rest x)
           (fn [new-tail]
             (snoc-cps new-tail (first x) k)))))



(comment
  (look-up {:a {:b {:c :d}}} [:a :b])
  (def wc (change-watch-fn [:a] println))
  (def b (atom {:a 1 :b 0}))
  (add-watch b :w wc)
  (swap! b assoc :b 1)
  (swap! b assoc :a 2)
  (swap! b assoc :a 3))
