(ns hara.reflect.hierarchy-test
  (:use midje.sweet)
  (:require [hara.reflect.hierarchy :refer :all]
            [hara.common.primitives :as common]))
  
(def without-method
    (-> clojure.lang.PersistentArrayMap
        (.getDeclaredMethod "without"
                            (hara.common.primitives/class-array [Object]))))

^{:refer hara.reflect.hierarchy/has-method :added "2.1"}
(fact "Checks to see if any given method exists in a particular class"

  (has-method without-method
              String)
  => nil

  (has-method without-method
              clojure.lang.PersistentArrayMap)
  => clojure.lang.PersistentArrayMap)

^{:refer hara.reflect.hierarchy/methods-with-same-name-and-count :added "2.1"}
(fact "methods-with-same-name-and-count"

  (methods-with-same-name-and-count without-method clojure.lang.IPersistentMap)
  =>  #(-> % count (= 1))  ;; (#<Method clojure.lang.IPersistentMap.without(java.lang.Object)>)

  ^:hidden
  (methods-with-same-name-and-count
   (.getDeclaredMethod String "charAt"
                       (hara.common.primitives/class-array Class [Integer/TYPE]))
   CharSequence)
  =>
  #(-> % count (= 1))  ;; (#<Method java.lang.CharSequence.charAt(int)>)
)

^{:refer hara.reflect.hierarchy/has-overridden-method :added "2.1"}
(fact "Checks to see that the method can be "

  (has-overridden-method without-method String)
  => nil

  (has-overridden-method without-method clojure.lang.IPersistentMap)
  => clojure.lang.IPersistentMap)

^{:refer hara.reflect.hierarchy/origins :added "2.1"}
(fact "Lists all the classes tha contain a particular method"

  (def without-method
    (-> clojure.lang.PersistentArrayMap
        (.getDeclaredMethod "without"
                            (hara.common.primitives/class-array [Object]))))

  (origins without-method)
  => [clojure.lang.IPersistentMap
      clojure.lang.PersistentArrayMap])
