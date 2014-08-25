(ns hara.data.nested-test
  (:use midje.sweet)
  (:require [hara.data.nested :refer :all]))

^{:refer hara.data.nested/keys-nested :added "2.1"}
(fact "The set of all nested keys in a map"

  (keys-nested {:a {:b 1 :c {:d 1}}})
  => #{:a :b :c :d})

^{:refer hara.data.nested/merge-nested :added "2.1"}
(fact "Merges nested values from left to right."

  (merge-nested {:a {:b {:c 3}}} {:a {:b 3}})
  => {:a {:b 3}}

  (merge-nested {:a {:b {:c 1 :d 2}}}
                {:a {:b {:c 3}}})
  => {:a {:b {:c 3 :d 2}}})

^{:refer hara.data.nested/merge-nil-nested :added "2.1"}
(fact "Merges nested values from left to right, provided the merged value does not exist"

  (merge-nil-nested {:a {:b 2}} {:a {:c 2}})
  => {:a {:b 2 :c 2}}

  (merge-nil-nested {:b {:c :old}} {:b {:c :new}})
  => {:b {:c :old}})

^{:refer hara.data.nested/dissoc-nested :added "2.1"}
(fact "Returns `m` without all nested keys in `ks`."

  (dissoc-nested {:a {:b 1 :c {:b 1}}} [:b])
  => {:a {:c {}}})

^{:refer hara.data.nested/unique-nested :added "2.1"}
(fact "All nested values in `m1` that are unique to those in `m2`."

  (unique-nested {:a {:b 1}}
               {:a {:b 1 :c 1}})
  => {}

  (unique-nested {:a {:b 1 :c 1}}
               {:a {:b 1}})
  => {:a {:c 1}})

^{:refer hara.data.nested/clean-nested :added "2.1"}
(fact "Returns a associative with nils and empty hash-maps removed."

   (clean-nested {:a {:b {:c {}}}})
   => {}

   (clean-nested {:a {:b {:c {} :d 1 :e nil}}})
   => {:a {:b {:d 1}}})
