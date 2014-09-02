(ns hara.sort.topological-test
  (:use midje.sweet)
  (:require [hara.sort.topological :refer :all]))

^{:refer hara.sort.topological/top-nodes :added "2.1"}
(fact "nodes that have no other nodes that are dependent on them"
  (top-nodes {:a #{} :b #{:a}})
  => #{:b})

^{:refer hara.sort.topological/topological-sort :added "2.1"}
(fact "sorts a directed graph into its dependency order"

  (topological-sort {:a #{:b :c},
                     :b #{:d :e},
                     :c #{:e :f},
                     :d #{},
                     :e #{:f},
                     :f nil})
  => [:f :d :e :b :c :a]

  (topological-sort {:a #{:b},
                     :b #{:a}})
  => (throws Exception "Graph Contains Circular Dependency: {:b #{:a}, :a #{:b}}"))
