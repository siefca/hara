(ns hara.test-propagator
  (:use midje.sweet
        hara.checkers
        hara.common)
  (:require [hara.propagator :as p]))



(defn under [num]
  (fn [p n] (or (< num n) (< num n))))


(def a (p/cell))
(def b (p/cell))
(def c (p/cell))
(def pg1 (connect '+ [a b] c +))
(def pg2 (connect 'inc [a] b inc))
(def pg3 (connect 'inc [c] a inc (under 10)))
(a 0)
b
