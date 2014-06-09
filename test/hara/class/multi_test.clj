(ns hara.class.multi-test
  (:use midje.sweet)
  (:require [hara.class.multi :refer :all :as class]))

(if (resolve 'display)
  (remove-classmulti display))

^{:refer hara.class.multi/defclassmulti :added "2.1"}
(fact "Defines class-based multimethod dispatch. Supporting methods are
  very similar to the built-in defmulti, although this will dispatch on
  the value of the class. Used mainly for metaprogramming."

  (defclassmulti  display [cls])
  (defclassmethod display CharSequence [cls] "Chars")
  (defclassmethod display Number [cls] "Number")
  (defclassmethod display Float  [cls] "Float")

  (display Float)  => "Float"
  (display Long)   => "Number"
  (display String) => "Chars"
  (display (type {})) => (throws Exception)
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
