(ns hara.common.test-keyword
  (:require [hara.common.keyword :as h]
            [midje.sweet :refer :all]))

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
