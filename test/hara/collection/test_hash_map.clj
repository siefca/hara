(ns hara.collection.test-hash-map
  (:require [hara.collection.hash-map :as h]
            [midje.sweet :refer :all]))

(fact "hashmap-ns will output all unique ns namespaces"
  (h/hashmap-ns {:hello/a 1 :hello/b 2 :a 3 :b 4}) => #{nil :hello}
  (h/hashmap-ns {:hello/a 1 :hello/b 2 :there/a 3 :there/b 4}) => #{:hello :there})

(fact "hashmap-ns?"
  (h/hashmap-ns? {:hello/a 1 :hello/b 2
                :there/a 3 :there/b 4} :not-there)
  => nil

  (h/hashmap-ns? {:hello/a 1 :hello/b 2
                :there/a 3 :there/b 4} :hello)
  => true)

(fact "hashmap-keys will output all keys within a namespace"
  (h/hashmap-keys {:hello/a 1 :hello/b 2 :there/a 3 :there/b 4})
  => {:there #{:there/a :there/b}, :hello #{:hello/b :hello/a}}

  (h/hashmap-keys {:hello/a 1 :hello/b 2 :there/a 3 :there/b 4} :hello)
  => #{:hello/a :hello/b})


(fact "flatten-keys will take a map of maps and make it into a single map"
  (h/flatten-keys {}) => {}
  (h/flatten-keys {:a 1 :b 2}) => {:a 1 :b 2}
  (h/flatten-keys {:a {:b 2 :c 3}}) => {:a/b 2 :a/c 3}
  (h/flatten-keys {:a {:b {:c 3 :d 4}
                             :e {:f 5 :g 6}}
                         :h {:i 7} })
  => {:a/b {:c 3 :d 4} :a/e {:f 5 :g 6} :h/i 7})

(fact "flatten-keys-nested will take a map of maps and make it into a single map"
  (h/flatten-keys-nested {}) => {}
  (h/flatten-keys-nested {:a 1 :b 2}) => {:a 1 :b 2}
  (h/flatten-keys-nested {:a {:b 2 :c 3}}) => {:a/b 2 :a/c 3}
  (h/flatten-keys-nested {:a {:b {:c 3 :d 4}
                        :e {:f 5 :g 6}}
                    :h {:i 7}})
  => {:a/b/c 3 :a/b/d 4 :a/e/f 5 :a/e/g 6 :h/i 7}
  (h/flatten-keys-nested {:a {}}) => {}
  (h/flatten-keys-nested {:a {:b {}}}) => {})

(fact "Adding another parameter to flatten-keys-nested will cause it to keep
       empty hashmaps"
  (h/flatten-keys-nested {} true) => {}
  (h/flatten-keys-nested {:a 1 :b 2} true) => {:a 1 :b 2}
  (h/flatten-keys-nested {:a {:b 2 :c 3}} true) => {:a/b 2 :a/c 3}
  (h/flatten-keys-nested {:a {:b {:c 3 :d 4}
                             :e {:f 5 :g 6}}
                         :h {:i 7}} true)
  => {:a/b/c 3 :a/b/d 4 :a/e/f 5 :a/e/g 6 :h/i 7}
  (h/flatten-keys-nested {:a {}} true) => {:a {}}
  (h/flatten-keys-nested {:a {:b {}}} true) => {:a/b {}})


(fact "treeify-keys will take a single map of compound keys and make it into a tree"
  (h/treeify-keys {}) => {}
  (h/treeify-keys {:a 1 :b 2}) => {:a 1 :b 2}
  (h/treeify-keys {:a/b 2 :a/c 3}) => {:a {:b 2 :c 3}}
  (h/treeify-keys {:a {:b/c 2} :a/d 3}) => {:a {:b/c 2 :d 3}}
  (h/treeify-keys {:a/b {:e/f 1} :a/c {:g/h 1}})
  => {:a {:b {:e/f 1} :c {:g/h 1}}}
  (h/treeify-keys {:a/b/c 3 :a/b/d 4 :a/e/f 5 :a/e/g 6 :h/i 7})
  => {:a {:b {:c 3 :d 4}
          :e {:f 5 :g 6}}
      :h {:i 7}})

(fact "treeify-keys will take a map of nested compound keys and shape it into a non-compound tree"
   (h/treeify-keys-nested nil) => {}
   (h/treeify-keys-nested {}) => {}
   (h/treeify-keys-nested {:a 1 :b 3}) => {:a 1 :b 3}
   (h/treeify-keys-nested {:a/b 2 :a {:c 3}}) => {:a {:b 2 :c 3}}
   (h/treeify-keys-nested {:a/b {:e/f 1} :a/c {:g/h 1}})
   => {:a {:b {:e {:f 1}}
           :c {:g {:h 1}}}}
   (h/treeify-keys-nested {:a {:b/c 2} :a/d 3}) => {:a {:b {:c 2} :d 3}})


(fact "nest-keys will extend a treeified map with given namespace keys"
  (h/nest-keys {:a 1 :b 2} [:hello] [])
  => {:hello {:a 1 :b 2}}

  (h/nest-keys {:a 1 :b 2} [:hello :there] [])
  => {:hello {:there {:a 1 :b 2}}}

  (h/nest-keys {:a 1 :b 2} [:hello] [:a])
  => {:hello {:b 2} :a 1}

  (h/nest-keys {:a 1 :b 2} [:hello] [:a :b])
  => {:hello {} :a 1 :b 2})

(fact "unnest-keys will make a treefied map"
  (h/unnest-keys {:hello/a 1 :hello/b 2
                  :there/a 3 :there/b 4} [:hello])
  => {:a 1 :b 2 :there {:a 3 :b 4}}

  (h/unnest-keys {:hello/a 1 :hello/b 2
                  :there/a 3 :there/b 4} [:hello])
  => {:a 1 :b 2 :there {:a 3 :b 4}}

  (h/unnest-keys {:hello {:a 1 :b 2}
                  :there {:a 3 :b 4}} [:hello])
  => {:a 1 :b 2 :there {:a 3 :b 4}}

  (h/unnest-keys {:hello/there/a 1 :hello/there/b 2
                  :again/there/a 3 :again/there/b 4}
                 [:hello :there])
  => {:a 1 :b 2 :again {:there {:a 3 :b 4}}}


  (h/unnest-keys {:hello {:there {:a 1 :b 2}}
                  :again {:there {:a 3 :b 4}}} [:hello :there])
  => {:a 1 :b 2 :again {:there {:a 3 :b 4}}}

  (h/unnest-keys {:hello/a 1 :hello/b 2
                  :there/a 3 :there/b 4} [:hello] [:+])
  => {:a 1 :b 2 :+ {:there {:a 3 :b 4}}})
