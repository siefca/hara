(ns hara.reflect.util-test
  (:use midje.sweet)
  (:require [hara.common.primitives :refer [class-array]]
            [hara.reflect.util :refer :all]))


^{:refer hara.reflect.util/box-arg :added "2.1"}
(fact "Converts primitives to their correct data types"
  (box-arg Float/TYPE 2)
  => 2.0

  (box-arg Integer/TYPE 2.001)
  => 2

  (type (box-arg Short/TYPE 1.0))
  => java.lang.Short)

^{:refer hara.reflect.util/more :added "2.1"}
(fact "param-arg-match basics"
  (.isPrimitive Integer/TYPE)
  => true

  (.isAssignableFrom Integer/TYPE Long/TYPE)
  => false

  (.isAssignableFrom Long/TYPE Integer/TYPE)
  => false

  (.isAssignableFrom java.util.Map clojure.lang.PersistentHashMap)
  => true

  (.isAssignableFrom clojure.lang.PersistentHashMap java.util.Map)
  => false)

^{:refer hara.reflect.util/param-arg-match :added "2.1"}
(fact "Checks if the second argument can be used as the first argument"
 (param-arg-match Double/TYPE Float/TYPE)
 => true

 (param-arg-match Float/TYPE Double/TYPE)
 => true

 (param-arg-match Integer/TYPE Float/TYPE)
 => false

 (param-arg-match Byte/TYPE Long/TYPE)
 => false

 (param-arg-match Long/TYPE Byte/TYPE)
 => true

 (param-arg-match Long/TYPE Long)
 => true

 (param-arg-match Long Byte)
 => false

 (param-arg-match clojure.lang.PersistentHashMap java.util.Map)
 => false

 (param-arg-match java.util.Map clojure.lang.PersistentHashMap)
 => true)
