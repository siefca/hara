(ns hara.common.string-test
  (:use midje.sweet)
  (:require [hara.common.string :refer :all]))

^{:refer hara.common.string/to-string :added "2.1"}
(fact "converts symbols and keywords to string representation"

  (to-string 'hello/world)
  => "hello/world"

  (to-string :hello/world)
  => "hello/world")

^{:refer hara.common.string/to-meta :added "2.1"}
(fact "meta information of keywords and symbols"

  (to-meta 'hello/world)
  => {:type clojure.lang.Symbol}

  (to-meta :hello/world)
  => {:type clojure.lang.Keyword})

^{:refer hara.common.string/from-string :added "2.1"}
(fact "meta information of keywords and symbols"

  (from-string {:type clojure.lang.Symbol} "hello/world")
  => 'hello/world

  (from-string {:type clojure.lang.Keyword} "hello/world")
  => :hello/world)
