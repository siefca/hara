(ns hara.common.checks-test
  (:use midje.sweet)
  (:require [hara.common.checks :refer :all]))

^{:refer hara.common.checks/boolean? :added "2.0"}
(fact "Returns `true` if `x` is of type `java.lang.Boolean`."

   (boolean? true)   => true
   (boolean? false)  => true

   ^:hidden
   (boolean? nil)    => false
   (boolean? 1)      => false)

^{:refer hara.common.checks/hash-map? :added "2.0"}
(fact "Returns `true` if `x` implements `clojure.lang.APersistentMap`."

   (hash-map? {})    => true
   (hash-map? [])    => false)

^{:refer hara.common.checks/long? :added "2.0"}
(fact "Returns `true` if `x` is of type `java.lang.Long`."

  (long? 1)          => true
  (long? 1N)         => false)

^{:refer hara.common.checks/bigint? :added "2.0"}
(fact "Returns `true` if `x` is of type `clojure.lang.BigInt`."

  (bigint? 1N)       => true
  (bigint? 1)        =>  false
)

^{:refer hara.common.checks/bigdec? :added "2.0"}
(fact "Returns `true` if `x` is of type `java.math.BigDecimal`."

  (bigdec? 1M)       => true
  (bigdec? 1.0)      => false)

^{:refer hara.common.checks/instant? :added "2.0"}
(fact "Returns `true` if `x` is of type `java.util.Date`."

  (instant? (java.util.Date.)) => true)

^{:refer hara.common.checks/uuid? :added "2.0"}
(fact "Returns `true` if `x` is of type `java.util.UUID`."

  (uuid? (java.util.UUID/randomUUID)) => true)

^{:refer hara.common.checks/uri? :added "2.0"}
(fact "Returns `true` if `x` is of type `java.net.URI`."

  (uri? (java.net.URI. "http://www.google.com")) => true)

^{:refer hara.common.checks/regex? :added "2.0"}
(fact "Returns `true` if `x` implements `clojure.lang.IPersistentMap`."

  (regex? #"\d+") => true
 )

^{:refer hara.common.checks/bytes? :added "2.0"}
(fact "Returns `true` if `x` is a primitive `byte` array."

  (bytes? (byte-array 8)) => true)

^{:refer hara.common.checks/atom? :added "2.0"}
(fact "Returns `true` if `x` is of type `clojure.lang.Atom`."

  (atom? (atom nil)) => true
)

^{:refer hara.common.checks/ref? :added "2.0"}
(fact "Returns `true` if `x` is of type `clojure.lang.Ref`."

  (ref? (ref nil)) => true
)

^{:refer hara.common.checks/agent? :added "2.0"}
(fact "Returns `true` if `x` is of type `clojure.lang.Agent`."

  (agent? (agent nil)) => true)

^{:refer hara.common.checks/iref? :added "2.0"}
(fact "Returns `true` if `x` is of type `clojure.lang.IRef`."

  (iref? (atom 0))  => true
  (iref? (ref 0))   => true
  (iref? (agent 0)) => true
  (iref? (promise)) => false
  (iref? (future))  => false)

^{:refer hara.common.checks/ideref? :added "2.0"}
(fact "Returns `true` if `x` is of type `java.lang.IDeref`."

  (ideref? (atom 0))  => true
  (ideref? (promise)) => true
  (ideref? (future))  => true)

^{:refer hara.common.checks/promise? :added "2.0"}
(fact "Returns `true` is `x` is a promise"

  (promise? (promise)) => true
  (promise? (future))  => false)

^{:refer hara.common.checks/interface? :added "2.1"}
(fact "Returns `true` if `class` is an interface"

  (interface? java.util.Map) => true
  (interface? Class) => false)
  
^{:refer hara.common.checks/abstract? :added "2.1"}
(fact "Returns `true` if `class` is an abstract class"

  (abstract? java.util.Map) => true
  (abstract? Class) => false)

^{:refer hara.common.checks/type-checker :added "2.0"}
(fact "Returns the checking function associated with `k`"

  (type-checker :string) => #'clojure.core/string?

  (require '[hara.common.checks :refer [bytes?]])
  (type-checker :bytes)  => #'hara.common.checks/bytes?)
