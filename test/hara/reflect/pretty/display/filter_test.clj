(ns hara.reflect.pretty.display.filter-test
  (:use midje.sweet)
  (:require [hara.reflect.pretty.display.filter :refer :all]))


^{:refer hara.reflect.pretty.display.filter/filter-terms-fn :added "2.1"}
(fact "listing outputs based upon different predicate conditions"

  ((filter-terms-fn {:name ["a"]})
   [{:name "a"} {:name "b"}])
  => [{:name "a"}]

  ((filter-terms-fn {:predicate [(fn [x] (= "a" (:name x)))]})
   [{:name "a"} {:name "b"}])
  => [{:name "a"}]

  ((filter-terms-fn {:origins [#{:a :b}]})
   [{:origins #{:a}} {:origins #{:c}}])
  => [{:origins #{:a}}]

  ((filter-terms-fn {:modifiers [:a]})
   [{:modifiers #{:a}} {:modifiers #{:c}}])
  => [{:modifiers #{:a}}])
