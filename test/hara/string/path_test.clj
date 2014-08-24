(ns hara.string.path-test
  (:use midje.sweet)
  (:require [hara.string.path :as path]))

^{:refer hara.string.path/join :added "2.1"}
(fact "joins a sequence of elements into a path seperated value"

  (path/join ["a" "b" "c"])
  => "a/b/c"

  (path/join '[a b c] '-)
  => 'a-b-c)

^{:refer hara.string.path/split :added "2.1"}
(fact "splits a sequence of elements into a path seperated value"

  (path/split :hello/world)
  => [:hello :world]

  (path/split "a/b/c/d")
  => '["a" "b" "c" "d"])

^{:refer hara.string.path/contains :added "2.1"}
(fact "check that a path contains the subkey"

  (path/contains :hello/world :hello)
  => true

  (path/contains "a/b/c/d" "a/b/c")
  => true)

^{:refer hara.string.path/path-vec :added "2.1"}
(fact "returns the path vector of the string/keyword/symbol"

  (path/path-vec "a/b/c/d")
  => ["a" "b" "c"])

^{:refer hara.string.path/path-vec? :added "2.1"}
(fact "check for the path vector of the string/keyword/symbol"

  (path/path-vec? "a/b/c/d" ["a" "b" "c"])
  => true)

^{:refer hara.string.path/path-ns :added "2.1"}
(fact "returns the path namespace of the string/keyword/symbol"

  (path/path-ns "a/b/c/d")
  => "a/b/c")

^{:refer hara.string.path/path-ns? :added "2.1"}
(fact "check for the path namespace of the string/keyword/symbol"

  (path/path-ns? "a/b/c/d" "a/b/c")
  => true)

^{:refer hara.string.path/path-root :added "2.1"}
(fact "returns the path root of the string/keyword/symbol"

  (path/path-root "a/b/c/d")
  => "a")

^{:refer hara.string.path/path-root? :added "2.1"}
(fact "check for the path root of the string/keyword/symbol"

  (path/path-root? "a/b/c/d" "a")
  => true)

^{:refer hara.string.path/path-stem-vec :added "2.1"}
(fact "returns the path stem vector of the string/keyword/symbol"

  (path/path-stem-vec "a/b/c/d")
  =>  ["b" "c" "d"])

^{:refer hara.string.path/path-stem-vec? :added "2.1"}
(fact "check for the path stem vector of the string/keyword/symbol"

  (path/path-stem-vec? "a/b/c/d" ["b" "c" "d"])
  => true)

^{:refer hara.string.path/path-stem :added "2.1"}
(fact "returns the path stem of the string/keyword/symbol"

  (path/path-stem "a/b/c/d")
  => "b/c/d")

^{:refer hara.string.path/path-stem? :added "2.1"}
(fact "check for the path stem of the string/keyword/symbol"

  (path/path-stem? "a/b/c/d" "b/c/d")
  => true)

^{:refer hara.string.path/val :added "2.1"}
(fact "returns the val of the string/keyword/symbol"

  (path/val "a/b/c/d")
  => "d")

^{:refer hara.string.path/val? :added "2.1"}
(fact "check for the val of the string/keyword/symbol"

  (path/val? "a/b/c/d" "d")
  => true)
