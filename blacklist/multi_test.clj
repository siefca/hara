(ns hara.class.multi-test
  (:use midje.sweet)
  (:require [hara.class.multi :refer :all :as class]))

(if (resolve 'display)
  (remove-classmulti display))

^{:refer hara.class.multi/defclassmulti :added "2.1"}
(fact "Defines class-based multimethod dispatch. Supporting methods are
  very similar to the built-in defmulti, although this will dispatch on
  the value of the class. Used mainly for metaprogramming."

  (defclassmulti  display [cls] (type cls))
  (defclassmethod display CharSequence [cls] "Chars")
  (defclassmethod display Number [cls] "Number")
  (defclassmethod display Double  [cls] "Double")

  (display 1.0)  => "Double"
  (display (long 1)) => "Number"
  (display "hello") => "Chars"
  (display {}) => (throws Exception)
  ^:hidden
  (remove-classmulti display))

^{:refer hara.class.multi/remove-classmulti :added "2.1"}
(fact "Uninstalls the classmulti method."

  (defclassmulti  example [cls])
  example => fn?

  (remove-classmulti example)
  example => #(instance? clojure.lang.Var$Unbound %))


^{:refer hara.class.multi/defclassmethod :added "2.1"}
(fact "Defines a class specific multimethod."

  (defclassmulti  display [cls m])
  (defclassmethod display Object [cls m] m)
  (display String {}) => {}

  (defclassmethod display String [cls m] (str m))
  (display String {}) => "{}"
  ^:hidden
  (remove-classmulti display))

^{:refer hara.class.multi/remove-classmethod :added "2.1"}
(fact "Removes a class specific multimethod."
  (defclassmulti  display [cls])
  (defclassmethod display Object [cls] "Object")
  (display String) => "Object"

  (remove-classmethod display Object)
  (display String) => (throws Exception)
  ^:hidden
  (remove-classmulti display))

^{:refer hara.class.multi/remove-all-classmethods :added "2.1"}
(fact "Removes all class specific multimethods."
  (defclassmulti  display [cls])
  (defclassmethod display Object [cls] "Object")
  (display String) => "Object"

  (remove-all-classmethods display)
  (display String) => (throws Exception)
  ^:hidden
  (remove-classmulti display))
  
^{:refer hara.class.multi/list-all-classmethods :added "2.1"}
(fact "Lists all class specific multimethods."
  (defclassmulti display [x])
  (defclassmethod display Object [x])
  (defclassmethod display String [x])
  
  (-> (list-all-classmethods display) keys)
  => (just [Object String] :in-any-order)
  ^:hidden
  (remove-classmulti display))
  
