(ns hara.class.inheritance-test
  (:use midje.sweet)
  (:require [hara.class.inheritance :as inheritance :refer [best-match]]))

^{:refer hara.class.inheritance/list :added "2.1"}
(fact "Lists the direct ancestors of a class"
  (inheritance/list clojure.lang.PersistentHashMap)
  => [clojure.lang.PersistentHashMap
      clojure.lang.APersistentMap
      clojure.lang.AFn
      java.lang.Object])

^{:refer hara.class.inheritance/tree :added "2.1"}
(fact "Lists the hierarchy of bases and interfaces of a class."
  (inheritance/tree Class)
  => [[java.lang.Object #{java.io.Serializable
                          java.lang.reflect.Type
                          java.lang.reflect.AnnotatedElement
                          java.lang.reflect.GenericDeclaration}]]
  ^:hidden
  (inheritance/tree clojure.lang.PersistentHashMap)
  => [[clojure.lang.APersistentMap #{clojure.lang.IEditableCollection
                                     clojure.lang.IObj}]
      [clojure.lang.AFn #{java.io.Serializable
                          clojure.lang.IHashEq
                          clojure.lang.MapEquivalence
                          java.util.Map
                          java.lang.Iterable
                          clojure.lang.IPersistentMap}]
      [java.lang.Object #{clojure.lang.IFn}]])

^{:refer hara.class.inheritance/best-match :added "2.1"}
(fact "finds the best matching interface or class from a list of candidates"

  (best-match #{Object} Long) => Object
  (best-match #{String} Long) => nil
  (best-match #{Object Number} Long) => Number)
