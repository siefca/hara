(ns hara.common.checks-test
  (:require [hara.common.checks :refer :all]
            [midje.sweet :refer :all]))

^{:refer hara.common.checks/boolean?
  :added "2.0.1"}
(fact "Returns `true` if `x` is of type `java.lang.Boolean`."

  (boolean? false) => true

  (boolean? nil) => false)

^{:refer hara.common.checks/hash-map?
  :added "2.0.1"}
(fact "Returns `true` if `x` implements `clojure.lang.IPersistentMap`."

  (hash-map? {}) => true

  (hash-map? []) => false

  ^:hidden
  (hash-map? {:a 1 :b 2}) => true)

^{:refer hara.common.checks/long?
  :added "2.0.1"}
(fact "Returns `true` if `x` is of type `java.lang.Long`."

  (long? 1) => true

  (long? 1N) => false)

(fact "type-predicates"
  (h/boolean? true) => true
  (h/boolean? false) => true
  (h/hash-map? {}) => true
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
