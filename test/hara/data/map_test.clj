(ns hara.data.map-test
  (:use midje.sweet)
  (:require [hara.data.map :refer :all]))

^{:refer hara.data.map/dissoc-in :added "2.1"}
(fact "disassociates keys from a nested map. Setting `keep` to `true` will
  not remove a empty map after dissoc"

  (dissoc-in {:a {:b 10 :c 20}} [:a :b])
  => {:a {:c 20}}

  (dissoc-in {:a {:b 10}} [:a :b])
  => {}

  (dissoc-in {:a {:b 10}} [:a :b] true)
  => {:a {}})


^{:refer hara.data.map/unique :added "2.1"}
(fact "returns a map of all key/value pairs that differ from a second map"

  (unique {:a 1} {:a 2})
  => {:a 1}

  (unique {:a 1 :b 2} {:b 2})
  => {:a 1}

  (unique {:b 2} {:b 2 :a 1})
  => nil)

^{:refer hara.data.map/assoc-if :added "2.1"}
(fact "assoc key/value pairs to the map only on non-nil values"

  (assoc-if {} :a 1)
  => {:a 1}

  (assoc-if {} :a 1 :b nil)
  => {:a 1})

^{:refer hara.data.map/assoc-in-if :added "2.1"}
(fact "assoc-in a nested key/value pair to a map only on non-nil values"

  (assoc-in-if {} [:a :b] 1)
  => {:a {:b 1}}

  (assoc-in-if {} [:a :b] nil)
  => {})

^{:refer hara.data.map/update-in-if :added "2.1"}
(fact "update-in a nested key/value pair only if the value exists"

  (update-in-if {:a {:b 1}} [:a :b] inc)
  => {:a {:b 2}}

  (update-in-if {} [:a :b] inc)
  => {})

^{:refer hara.data.map/merge-if :added "2.1"}
(fact "merges key/value pairs into a single map only if the value exists"

  (merge-if {:a nil :b 1})
  => {:b 1}

  (merge-if {:a 1} {:b nil :c 2})
  => {:a 1 :c 2}

  (merge-if {:a 1} {:b nil} {:c 2})
  => {:a 1 :c 2})


^{:refer hara.data.map/into-if :added "2.1"}
(fact "like into but filters nil values for both key/value pairs
  and sequences"

  (into-if [] [1 nil 2 3])
  => [1 2 3]

  (into-if {:a 1} {:b nil :c 2})
  => {:a 1 :c 2})

^{:refer hara.data.map/select-keys-if :added "2.1"}
(fact "selects only the non-nil key/value pairs from a map"

  (select-keys-if {:a 1 :b nil} [:a :b])
  => {:a 1}

  (select-keys-if {:a 1 :b nil :c 2} [:a :b :c])
  => {:a 1 :c 2})

^{:refer hara.data.map/merge-nil :added "2.1"}
(fact "only merge if the value in the original map is nil"

  (merge-nil {:a 1} {:b 2})
  => {:a 1 :b 2}

  (merge-nil {:a 1} {:a 2})
  => {:a 1})

^{:refer hara.data.map/assoc-nil :added "2.1"}
(fact "only assoc if the value in the original map is nil"

  (assoc-nil {:a 1} :b 2)
  => {:a 1 :b 2}

  (assoc-nil {:a 1} :a 2)
  => {:a 1})

^{:refer hara.data.map/assoc-in-nil :added "2.1"}
(fact "only assoc-in if the value in the original map is nil"

  (assoc-in-nil {} [:a :b] 2)
  => {:a {:b 2}}

  (assoc-in-nil {:a {:b 1}} [:a :b] 2)
  => {:a {:b 1}})
