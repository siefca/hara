(ns hara.core-test
  (:use midje.sweet)
  (:require [hara.core :refer :all])
  (:refer-clojure :exclude [if-let when-let comp]))

^{:refer hara.core/comp :added "2.0"}
(fact "Same as `clojure.core/comp` except that the functions will shortcircuit on nil input."

  ((comp inc inc) 1) => 3
  ((comp inc inc) nil) => nil
)

^{:refer hara.core/if-let :added "2.0"}
(fact "Same as `clojure.core/if-let` except that more than one var can be binded.
  If any of the bounded vars are `nil`, then the expression will short circuit to the other branch"

  (if-let [a 1 b 2] (+ a b)) => 3

  (if-let [a nil b 2] (+ a b) :nil) => :nil)

^{:refer hara.core/when-let :added "2.0"}
(fact "Same as `clojure.core/when-let` except that more than one var can be binded.
  If any of the bounded vars are `nil`, then the expression will short circuit to return nil"

  (when-let [a 1 b 2] (+ a b)) => 3

  (when-let [a nil b 2] (+ a b)) => nil)
