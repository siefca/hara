(ns hara.test-utils
  (:use midje.sweet
        [hara.utils :only [bytes?]])
  (:require [hara.utils :as h]))

(fact "type-checker"
  (h/type-checker :string) => (exactly #'clojure.core/string?)
  (h/type-checker :bytes) =>  (exactly #'hara.utils/bytes?)
  (h/type-checker :other) =>  nil)

(fact "error"
  (h/error "something") => throws Exception)

(fact "suppress"
  (h/suppress 2) => 2
  (h/suppress (h/error "e")) => nil)

(fact "func-map creates a hashmap using as key the function applied to each
       element of the collection."
  (h/func-map identity [1 2 3]) => {1 1 2 2 3 3}
  (h/func-map #(* 2 %) [1 2 3]) => {2 1 4 2 6 3}
  (h/func-map #(* 2 %) [1 1 1]) => {2 1}

  (h/func-map :id [{:id 1 :val 1} {:id 2 :val 2}])
  => {1 {:id 1 :val 1} 2 {:id 2 :val 2}}

  "Same :ids will cause the first to be overwritten"
  (h/func-map :id [{:id 1 :val 1} {:id 1 :val 2}])
  => {1 {:id 1 :val 2}})

(fact "remove-repeats outputs a filtered list of values"
  (h/remove-repeats [1 1 2 2 3 3 4 5 6]) => [1 2 3 4 5 6]
  (h/remove-repeats :n [{:n 1} {:n 1} {:n 1} {:n 2} {:n 2}]) => [{:n 1} {:n 2}]
  (h/remove-repeats even? [2 4 6 1 3 5]) => [2 1])

(fact "replace-walk"
  (h/replace-walk 1 {1 2})
  => 2

  (h/replace-walk [1 2 3] {1 2})
  => [2 2 3]

  (h/replace-walk '[1 (1 2 [1 2 3])] {1 3 3 1})
  => '[3 (3 2 [3 2 1])]

  (h/replace-walk {:a 1 :b {:c 1}} {1 2})
  => {:a 2 :b {:c 2}}

  (h/replace-walk '{:a 1 :b {:c [1 (1 [1 1])]}} {1 2})
  => '{:a 2 :b {:c [2 (2 [2 2])]}})

(fact "dissoc-in"
  (h/dissoc-in {:a 2 :b 2} [:a]) => {:b 2}
  (h/dissoc-in {:a 2 :b 2} [:a] true) => {:b 2}

  (h/dissoc-in {:a {:b 2 :c 3}} [:a :b]) => {:a {:c 3}}
  (h/dissoc-in {:a {:b 2 :c 3}} [:a :b] true) => {:a {:c 3}}

  (h/dissoc-in {:a {:c 3}} [:a :c]) => {}
  (h/dissoc-in {:a {:c 3}} [:a :c] true) => {:a {}}

  (h/dissoc-in {:a {:b {:c 3}}} [:a :b :c]) => {}
  (h/dissoc-in {:a {:b {:c 3}}} [:a :b :c] true) => {:a {:b {}}})

(fact "keys-nested will output all keys in a map"
  (h/keys-nested {:a {:b 1 :c 2}})
  => #{:a :b :c}
  (h/keys-nested {:a {:b 1 :c {:d 2 :e {:f 3}}}})
  => #{:a :b :c :d :e :f})

(fact "dissoc-nested will dissoc all nested keys in a map"
  (h/dissoc-nested {:a 1} [:a])
  => {}

  (h/dissoc-nested {:a 1 :b 1} [:a :b])
  => {}

  (h/dissoc-nested {:a {:b 1 :c {:b 1}}} [:b])
  => {:a {:c {}}}

  (h/dissoc-nested {:a {:b 1 :c {:b 1}}} [:a :b])
  => {})


(fact "diff-nested will take two maps and compare what in the first is different to that in the second"
  (h/diff-nested {} {}) => {}
  (h/diff-nested {:a 1} {}) => {:a 1}
  (h/diff-nested {:a {:b 1}} {})=> {:a {:b 1}}
  (h/diff-nested {:a {:b 1}} {:a {:b 1}}) => {}
  (h/diff-nested {:a {:b 1}} {:a {:b 1 :c 1}}) => {}
  (h/diff-nested {:a {:b 1 :c 1}} {:a {:b 1}}) => {:a {:c 1}}
  (h/diff-nested {:a 1 :b {:c {:d {:e 1}}}}
                {:a 1 :b {:c {:d {:e 1}}}})
  => {}
  (h/diff-nested {:a 1 :b {:c {:d {:e 1}}}}
                {:a 1 :b {:c 1}})
  => {:b {:c {:d {:e 1}}}})


(fact "merge-nested will take two maps and merge them recursively"
  (h/merge-nested {} {}) => {}
  (h/merge-nested {:a 1} {}) => {:a 1}
  (h/merge-nested {} {:a 1}) => {:a 1}
  (h/merge-nested {:a {:b 1}} {:a {:c 2}}) => {:a {:b 1 :c 2}}
  (h/merge-nested {:a {:b {:c 1}}} {:a {:b {:c 2}}}) => {:a {:b {:c 2}}}
  (h/merge-nested {:a {:b 3}} {:a {:b {:c 3}}}) => {:a {:b {:c 3}}}
  (h/merge-nested {:a {:b {:c 3}}} {:a {:b 3}}) => {:a {:b 3}}
  (h/merge-nested {:a {:b {:c 1 :d 2}}} {:a {:b {:c 3}}}) => {:a {:b {:c 3 :d 2}}})


(fact "remove-nested"
  (h/remove-nested {}) => {}
  (h/remove-nested {:a {}}) => {}
  (h/remove-nested {:a {} :b 1}) => {:b 1}
  (h/remove-nested {:a {:b {:c 1}}}) => {:a {:b {:c 1}}}
  (h/remove-nested {:a {:b {:c {}}}}) => {}
  (h/remove-nested {:a {:b {:c {} :d 1}}}) => {:a {:b {:d 1}}})

(fact "keyword-str returns the string representation with the colon"
  (h/keyword-str nil) => ""
  (h/keyword-str :hello) => "hello"
  (h/keyword-str :hello/there) => "hello/there"
  (h/keyword-str :hello/there/man) => "hello/there/man")

(fact "keyword-join merges a sequence of keys together to form a keyword"
  (h/keyword-join []) => nil
  (h/keyword-join [:hello]) => :hello
  (h/keyword-join [:hello :there]) => :hello/there
  (h/keyword-join [:a :b :c :d]) => :a/b/c/d)

(fact "keyword-split does the inverse of keyword-join.
       It takes a keyword and turns it into a vector"
  (h/keyword-split nil) => []
  (h/keyword-split :hello) => [:hello]
  (h/keyword-split :hello/there) => [:hello :there]
  (h/keyword-split :a/b/c/d) => [:a :b :c :d])

(fact "keyword-contains?"
  (h/keyword-contains? nil nil) => true
  (h/keyword-contains? nil :nil) => false
  (h/keyword-contains? (keyword "") nil) => false
  (h/keyword-contains? :hello :hello) => true
  (h/keyword-contains? :hello/there :hello) => true
  (h/keyword-contains? :hellothere :hello) => false
  (h/keyword-contains? :hello/there/again :hello) => true
  (h/keyword-contains? :hello/there/again :hello/there) => true)

(fact "keyword-nsvec will output the namespace in vector form"
  (h/keyword-nsvec nil) => []
  (h/keyword-nsvec :hello) => []
  (h/keyword-nsvec :hello/there) => [:hello]
  (h/keyword-nsvec :hello/there/again) => [:hello :there])

(fact "keyword-nsvec? will check is the output of key namespace is the that specified"
  (h/keyword-nsvec? nil []) => true
  (h/keyword-nsvec? :hello []) => true
  (h/keyword-nsvec? :hello/there [:hello]) => true
  (h/keyword-nsvec? :hello/there/again [:hello :there]) => true
  (h/keyword-nsvec? :hello/there/again [:hello]) => false)

(fact "keyword-ns will output the namespace of a key"
  (h/keyword-ns nil) => nil
  (h/keyword-ns :hello) => nil
  (h/keyword-ns :hello/there) => :hello
  (h/keyword-ns :hello/there/again) => :hello/there)

(fact "keyword-ns? will check if the key contains a namespace"
  (h/keyword-ns? nil) => false
  (h/keyword-ns? nil nil) => true
  (h/keyword-ns? :hello) => false
  (h/keyword-ns? :hello nil) => true
  (h/keyword-ns? :hello/there) => true
  (h/keyword-ns? :hello/there :hello) => true
  (h/keyword-ns? :hello/there/again) => true
  (h/keyword-ns? :hello/there/again :hello/there) => true)

(fact "keyword-nsroot will output the keyword root"
  (h/keyword-root nil) => nil
  (h/keyword-root :hello) => nil
  (h/keyword-root :hello/there) => :hello
  (h/keyword-root :hello/there/again) => :hello)

(fact "keyword-nsroot? will check is the output of key namespace is the that specified"
  (h/keyword-root? nil nil) => true
  (h/keyword-root? :hello nil) => true
  (h/keyword-root? :hello/there :hello) => true
  (h/keyword-root? :hello/there/again :hello) => true)

(fact "keyword-stemvec will output the namespace in vector form"
  (h/keyword-stemvec nil) => []
  (h/keyword-stemvec :hello) => []
  (h/keyword-stemvec :hello/there) => [:there]
  (h/keyword-stemvec :hello/there/again) => [:there :again])

(fact "keyword-stemvec? will check is the output of key namespace is the that specified"
  (h/keyword-stemvec? nil []) => true
  (h/keyword-stemvec? :hello []) => true
  (h/keyword-stemvec? :hello/there [:there]) => true
  (h/keyword-stemvec? :hello/there/again [:there :again]) => true)

(fact "keyword-stem will output the namespace in tor form"
  (h/keyword-stem nil) => nil
  (h/keyword-stem :hello) => nil
  (h/keyword-stem :hello/there) => :there
  (h/keyword-stem :hello/there/again) => :there/again)

(fact "keyword-stem? will check is the output of key namespace is the that specified"
  (h/keyword-stem? nil nil) => true
  (h/keyword-stem? :hello nil) => true
  (h/keyword-stem? :hello/there :there) => true
  (h/keyword-stem? :hello/there/again :there/again) => true)

(fact "keyword-val will output the last "
  (h/keyword-val nil) => nil
  (h/keyword-val :hello) => :hello
  (h/keyword-val :hello/there) => :there
  (h/keyword-val :hello/there/again) => :again)

(fact "keyword-val will check if the output is that specified"
  (h/keyword-val? nil nil) => true
  (h/keyword-val? :hello :hello) => true
  (h/keyword-val? :hello/there :there) => true
  (h/keyword-val? :hello/there/again :again) => true)


(fact "datmap-ns will output all unique kns namespace"
  (h/datmap-ns {:hello/a 1 :hello/b 2 :a 3 :b 4}) => #{nil :hello}
  (h/datmap-ns {:hello/a 1 :hello/b 2 :there/a 3 :there/b 4}) => #{:hello :there})

(fact "datmap-ns?"
  (h/datmap-ns? {:hello/a 1 :hello/b 2
                :there/a 3 :there/b 4} :not-there)
  => nil

  (h/datmap-ns? {:hello/a 1 :hello/b 2
                :there/a 3 :there/b 4} :hello)
  => true)

(fact "datmap-keys will output all keys"
  (h/datmap-keys {:hello/a 1 :hello/b 2 :there/a 3 :there/b 4})
  => {:there #{:there/a :there/b}, :hello #{:hello/b :hello/a}}

  (h/datmap-keys {:hello/a 1 :hello/b 2 :there/a 3 :there/b 4} :hello)
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

(fact "combine"
  (h/combine nil nil) => nil
  (h/combine 1 1) => 1
  (h/combine 1 2) => #{1 2}
  (h/combine #{1} 1) => #{1}
  (h/combine 1 nil) => #{1 nil}
  (h/combine 1 #{1}) => #{1}
  (h/combine 1 #{2}) => #{1 2}
  (h/combine 1 #{nil}) => #{1 nil})

(fact "combine-obj"
  (h/combine-obj #{1 2 3} 2 identity)
  => 2
  (h/combine-obj #{1 2 3} 4 identity)
  => nil
  (h/combine-obj #{{:id 1}} {:id 1 :val 1} :id)
  => {:id 1})

(fact "combine-to-set"
  (h/combine-to-set #{{:id 1} {:id 2}}
                    {:id 3}
                    :id merge)
  => #{{:id 1} {:id 2} {:id 3}}
  (h/combine-to-set #{{:id 1} {:id 2}}
                    {:id 1 :val 1}
                    :id merge)
  => #{{:id 1 :val 1} {:id 2}})

(fact "combine-sets"
  (h/combine-sets #{{:id 1} {:id 2}}
                  #{{:id 1 :val 1} {:id 2 :val 2}}
                  :id merge)
  => #{{:id 1 :val 1} {:id 2 :val 2}})

(fact "combine"
  (h/combine #{{:id 1} {:id 2}}
             {:id 1 :val 1}
             :id merge)
  => #{{:val 1, :id 1} {:id 2}}

  (h/combine {:id 1 :val 1}
             #{{:id 1} {:id 2}}
             :id merge)
  => #{{:val 1, :id 1} {:id 2}}

  (h/combine #{{:id 1} {:id 2}}
             #{{:id 1 :val 1} {:id 2 :val 2}}
             :id merge)
  => #{{:id 1 :val 1} {:id 2 :val 2}}

  (h/combine {:id 1 :val 1}
             {:id 1}
             :id merge)
  => {:id 1 :val 1}

  (h/combine {:id 1 :val 1}
             {:id 2}
             :id merge)
  => #{{:val 1, :id 1} {:id 2}})

(fact "decombine"
  (h/decombine #{1 2 3 4} 1)
  => #{2 3 4}
  (h/decombine #{1 2 3 4} #{1 2})
  => #{3 4}
  (h/decombine #{1 2 3 4} even?)
  => #{1 3}
  (h/decombine #{{:id 1} {:id 2}}
               #(= 1 (:id %)))
  => #{{:id 2}})

(fact "assocs will merge values into a set when added"
  (h/assocs {} :b 1) => {:b 1}
  (h/assocs {:a 1} :b 1) => {:a 1 :b 1}
  (h/assocs {:a 1} :a 1) => {:a 1}
  (h/assocs {:a 1} :a 2) => {:a #{1 2}}
  (h/assocs {:a #{1}} :a 2) => {:a #{1 2}}
  (h/assocs {:a 1} :a #{2}) => {:a #{1 2}})

(fact "assocs will also do more complicated merges"
  (h/assocs {:a {:id 1}} :a {:id 1 :val 1} :id merge)
  => {:a {:val 1, :id 1}}

  (h/assocs {:a {:id 1}} :a {:id 2} :id merge)
  {:a #{{:id 1} {:id 2}}})

(fact "dissocs will unmerge "
  (h/dissocs {:a 1} :a) => {}
  (h/dissocs {:a 1} [:a #{2}]) => {:a 1}
  (h/dissocs {:a 1} [:a #{1}]) => {}
  (h/dissocs {:a 1} [:a #{1 2}]) => {}
  (h/dissocs {:a #{1 2}} [:a 1]) => {:a #{2}}
  (h/dissocs {:a #{1 2}} [:a 0]) => {:a #{1 2}}
  (h/dissocs {:a #{1 2}} [:a #{1}]) => {:a #{2}}
  (h/dissocs {:a #{1 2}} [:a #{0 1}]) => {:a #{2}}
  (h/dissocs {:a #{1 2}} [:a #{1 2}]) => {}
  (h/dissocs {:a #{1 2}} [:a #{1 2}]) => {}
  (h/dissocs {:a #{1 2}} [:a even?]) => {:a #{1}}
  (h/dissocs {:a #{{:id 1} {:id 2}}} [:a #(= 1 (:id %))]) => {:a #{{:id 2}}}
  (h/dissocs {:a 1 :b 2} :a :b) => {})
