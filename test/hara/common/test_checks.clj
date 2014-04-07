(ns hara.common.test_checks
  (:require [hara.common.checks :as h :refer [bytes?]]
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
  (h/promise? (promise)) => true)

(fact "type-checker"
  (h/type-checker :string) => (exactly #'clojure.core/string?)
  (h/type-checker :bytes) =>  (exactly #'hara.common.checks/bytes?)
  (h/type-checker :other) =>  nil)
