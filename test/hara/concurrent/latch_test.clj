(ns hara.concurrent.latch-test
  (:use midje.sweet)
  (:require [hara.concurrent.latch :refer :all]))

^{:refer hara.concurrent.latch/latch :added "2.1"}
(fact "Followes two irefs together so that when `master`
  changes, the `slave` will also be updated."

  (def master (atom 1))
  (def slave (atom nil))

  (latch master slave #(* 10 %))
  (swap! master inc)

  @master => 2
  @slave => 20)

^{:refer hara.concurrent.latch/unlatch :added "2.1"}
(fact "Removes the latch so that updates will not be propagated"

  (def master (atom 1))
  (def slave (atom nil))

  (latch master slave)
  (swap! master inc)
  @master => 2
  @slave => 2
  
  (unlatch master slave)
  (swap! master inc)
  @master => 3
  @slave => 2)
