(ns hara.concurrent.propagate-test
  (:use midje.sweet)
  (:require [hara.concurrent.propagate :refer :all]))


^{:refer hara.concurrent.propagate/cell :added "2.1"}
(fact "creates a propogation cell"

  (def cell-a (cell))
  @cell-a => :hara.concurrent.propagate/nothing

  (def cell-b (cell "Hello"))
  @cell-b => "Hello"

  (cell-b "World")    ;; invoking sets the state of the cell
  @cell-b => "World")

^{:refer hara.concurrent.propagate/link :added "2.1"}
(fact "creates a propogation link between a set of input cells and an output cell"

  (def in-a  (cell 1))
  (def in-b  (cell 2))
  (def inter (cell))
  (def in-c  (cell 3))
  (def out   (cell))

  (link [in-a in-b] inter +)
  (link [inter in-c] out +)

  (in-a 10)
  @inter => 12
  @out => 15

  (in-b 100)
  @inter => 110
  @out => 113

  (in-c 1000)
  @inter => 110
  @out => 1110)


^{:refer hara.concurrent.propagate/unlink :added "2.1"}
(fact "removes the propagation link between a set of cells"

  (def in-a  (cell 1))
  (def out   (cell))

  (def lk (link [in-a] out))
  (in-a 10)
  @out => 10

  (unlink lk)
  (in-a 100)
  @in-a 100
  @out => 10)
