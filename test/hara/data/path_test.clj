(ns hara.data.path-test
  (:use midje.sweet)
  (:require [hara.data.path :refer :all]))

^{:refer hara.data.path/list-ns-keys :added "2.1"}
(fact "Returns the set of keyword namespaces within a map"

  (list-ns-keys {:hello/a 1 :hello/b 2
                 :there/a 3 :there/b 4})
  => #{:hello :there})

^{:refer hara.data.path/contains-ns-key? :added "2.1"}
(fact "Returns `true` if any key in map contains a namespace value"

  (contains-ns-key? {:hello/a 1 :hello/b 2
                     :there/a 3 :there/b 4} :hello)
  => true)

^{:refer hara.data.path/group-by-set :added "2.1"}
(fact "Returns a map of the elements of coll keyed by the result of
  f on each element. The value at each key will be a set of the
  corresponding elements, in the order they appeared in coll."

  (group-by-set even? [1 2 3 4 5])
  => {false #{1 3 5}, true #{2 4}} )

^{:refer hara.data.path/group-keys :added "2.1"}
(fact "Returns the set of keys in `fm` that has keyword namespace
  of `ns`"
  (group-keys {:hello/a 1 :hello/b 2
               :there/a 3 :there/b 4})
  => {:there #{:there/a :there/b}, :hello #{:hello/b :hello/a}}

  (group-keys {:hello/a 1 :hello/b 2
               :there/a 3 :there/b 4} :hello)
  => #{:hello/a :hello/b})

^{:refer hara.data.path/flatten-keys :added "2.1"}
(fact "takes map `m` and flattens the first nested layer onto the root layer."

  (flatten-keys {:a {:b 2 :c 3} :e 4})
  => {:a/b 2 :a/c 3 :e 4}

  (flatten-keys {:a {:b {:c 3 :d 4}
                     :e {:f 5 :g 6}}
                 :h {:i 7}
                 :j 8})
  => {:a/b {:c 3 :d 4} :a/e {:f 5 :g 6} :h/i 7 :j 8})

^{:refer hara.data.path/flatten-keys-nested :added "2.1"}
(fact "Returns a single associative map with all of the nested
   keys of `m` flattened. If `keep` is added, it preserves all the
   empty sets"

  (flatten-keys-nested {"a" {"b" {"c" 3 "d" 4}
                               "e" {"f" 5 "g" 6}}
                          "h" {"i" {}}})
  => {"a/b/c" 3 "a/b/d" 4 "a/e/f" 5 "a/e/g" 6}

  (flatten-keys-nested {"a" {"b" {"c" 3 "d" 4}
                               "e" {"f" 5 "g" 6}}
                          "h" {"i" {}}}
                       true)
  => {"a/b/c" 3 "a/b/d" 4 "a/e/f" 5 "a/e/g" 6 "h/i" {}})

^{:refer hara.data.path/treeify-keys :added "2.1"}
(fact "Returns a nested map, expanding out the first
   level of keys into additional hash-maps."

  (treeify-keys {:a/b 2 :a/c 3})
  => {:a {:b 2 :c 3}}

  (treeify-keys {:a/b {:e/f 1} :a/c {:g/h 1}})
  => {:a {:b {:e/f 1}
          :c {:g/h 1}}})

^{:refer hara.data.path/treeify-keys-nested :added "2.1"}
(fact "Returns a nested map, expanding out all
 levels of keys into additional hash-maps."

  (treeify-keys-nested {:a/b 2 :a/c 3})
  => {:a {:b 2 :c 3}}

  (treeify-keys-nested {:a/b {:e/f 1} :a/c {:g/h 1}})
  => {:a {:b {:e {:f 1}}
          :c {:g {:h 1}}}})

^{:refer hara.data.path/nest-keys :added "2.1"}
(fact "Returns a map that takes `m` and extends all keys with the
  `nskv` vector. `ex` is the list of keys that are not extended."

  (nest-keys {:a 1 :b 2} [:hello :there])
   => {:hello {:there {:a 1 :b 2}}}

   (nest-keys {:there 1 :b 2} [:hello] [:there])
   => {:hello {:b 2} :there 1})

^{:refer hara.data.path/unnest-keys :added "2.1"}
(fact "The reverse of `nest-keys`. Takes `m` and returns a map
  with all keys with a `keyword-nsvec` of `nskv` being 'unnested'"

  (unnest-keys {:hello/a 1
                :hello/b 2
                :there/a 3
                :there/b 4} [:hello])
  => {:a 1 :b 2
      :there {:a 3 :b 4}}

  (unnest-keys {:hello {:there {:a 1 :b 2}}
                :again {:c 3 :d 4}} [:hello :there] [:+] )
  => {:a 1 :b 2
      :+ {:again {:c 3 :d 4}}})
