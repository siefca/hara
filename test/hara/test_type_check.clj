(ns hara.test-type-check
  (:require [hara.type-check :as h :refer [bytes?]]
            [hara.common :as s]
            [midje.sweet :refer :all]))
            

(fact "type-predicates"
  (h/boolean? true) => true
  (h/boolean? false) => true
  (h/hash-map? {}) => true
  (h/hash-set? #{}) => true
  (h/long? 1) => true
  (h/long? 1N) => false
  (h/bigint? 1N) => true
  (h/bigdec? 1M) => true
  (h/instant? (s/instant 0)) => true
  (h/uuid? (s/uuid)) => true
  (h/bytes? (byte-array 8)) => true)

(fact "atom?"
  (h/atom? (atom 0)) => true)

(fact "aref?"
  (h/aref? (ref 0)) => true)

(fact "iref?"
  (h/iref? (atom 0)) => true
  (h/iref? (ref 0)) => true)

(fact "ideref?"
  (h/ideref? (atom 0)) => true
  (h/ideref? (ref 0)) => true
  (h/ideref? (promise)) => true)

(fact "promise?"
  (h/promise? (future (inc 1))) => true)

(fact "type-checker"
  (h/type-checker :string) => (exactly #'clojure.core/string?)
  (h/type-checker :bytes) =>  (exactly #'hara.type-check/bytes?)
  (h/type-checker :other) =>  nil)
