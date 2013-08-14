(ns hara.common.test-debug
  (:require [hara.common.debug :as h]
            [midje.sweet :refer :all]))

(defn within-interval [min max]
  (fn [x]
    (and (> x min) (< x max))))

(defn approx
  [val error] (within-interval (- val error) (+ val error)))

(fact "time-ms"
  (h/time-ms (inc 1)) => (approx 0 0.02)
  (h/time-ms (Thread/sleep 100)) => (approx 100 2))

