(ns hara.string.case-test
  (:use midje.sweet)
  (:require [hara.string.case :refer :all]))

^{:refer hara.string.case/title-case :added "2.1"}
(fact "converts a string-like object to a title case string"

  (title-case "helloWorld")

  => "Hello World"

  (title-case :hello-world)
  => "Hello World")

^{:refer hara.string.case/lower-case :added "2.1"}
(fact "converts a string-like object to a lower case string"

  (lower-case "helloWorld")
  => "hello world"

  (lower-case 'hello-world)
  => "hello world")

^{:refer hara.string.case/camel-case :added "2.1"}
(fact "converts a string-like object to camel case representation"

  (camel-case :hello-world)
  => :helloWorld

  (camel-case 'hello_world)
  => 'helloWorld)

^{:refer hara.string.case/snake-case :added "2.1"}
(fact "converts a string-like object to snake case representation"

  (snake-case :hello-world)
  => :hello_world

  (snake-case 'helloWorld)
  => 'hello_world)

^{:refer hara.string.case/spear-case :added "2.1"}
(fact "converts a string-like object to spear case representation"

  (spear-case :hello_world)
  => :hello-world

  (spear-case 'helloWorld)
  => 'hello-world)
