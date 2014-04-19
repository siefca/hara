(ns hara.common.primitives-test
  (:use midje.sweet)
  (:require [hara.common.primitives :refer :all]))

^{:refer hara.common.primitives/T :added "2.0"}
(fact "Returns `true` for any combination of input `args`"

  (T) => true
  (T :hello) => true
  (T 1 2 3) => true)

^{:refer hara.common.primitives/F :added "2.0"}
(fact "Returns `false` for any combination of input `args`"

  (F) => false
  (F :hello) => false
  (F 1 2 3) => false)

^{:refer hara.common.primitives/NIL :added "2.0"}
(fact "Returns `nil` for any combination of input `args`"

  (NIL) => nil
  (NIL :hello) => nil
  (NIL 1 2 3) => nil)

^{:refer hara.common.primitives/queue :added "2.0"}
(fact "Returns a `clojure.lang.PersistentQueue` object."

  (def a (queue 1 2 3 4))
  (pop a) => [2 3 4])

^{:refer hara.common.primitives/uuid :added "2.0"}
(fact "Returns a `java.util.UUID` object"

  (uuid) => #(instance? java.util.UUID %) ; <random uuid>

  (uuid "00000000-0000-0000-0000-000000000000")
  => #uuid "00000000-0000-0000-0000-000000000000")

^{:refer hara.common.primitives/instant :added "2.0"}
(fact "Returns a `java.util.Date` object"

  (instant) => #(instance? java.util.Date %) ; <current time>

  (instant 0) => #inst "1970-01-01T00:00:00.000-00:00")

^{:refer hara.common.primitives/uri :added "2.0"}
(fact "Returns a `java.net.URI` object"

  (uri "http://www.google.com")
  => #(instance? java.net.URI %))
