(ns hara.collection.test-data-map
  (:require [hara.collection.data-map :as h]
            [hara.common.types :refer [hash-map?]]
            [midje.sweet :refer :all]))

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

(fact "combine-interval"
  (h/combine-internal #{{:id 1} {:id 2} {:id 1 :val 1} {:id 2 :val 2}}
                      :id merge)
  => #{{:id 1 :val 1} {:id 2 :val 2}})

(fact "combine"
  (h/combine nil nil) => nil
  (h/combine 1 1) => 1
  (h/combine 1 2) => #{1 2}
  (h/combine #{1} 1) => #{1}
  (h/combine 1 nil) => 1
  (h/combine 1 #{1}) => #{1}
  (h/combine 1 #{2}) => #{1 2}
  (h/combine 1 #{}) => #{1})

(fact "combine"
  (h/combine 4 2 even? max) => 4
  (h/combine #{4} 2 even? max) => #{4}
  (h/combine 4 #{2} even? max) => #{4}
  (h/combine #{4} #{2} even? max) => #{4}
  (h/combine #{4} #{2 6} even? max) => #{6}
  (h/combine #{4 6} #{2} even? max) => #{6}

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
  (h/decombine 1 1) => nil
  (h/decombine 1 #{1}) => nil
  (h/decombine 1 2) => 1
  (h/decombine #{1} 1) => nil
  (h/decombine #{1 2 3 4} 1) => #{2 3 4}
  (h/decombine #{1 2 3 4} #{1 2}) => #{3 4}
  (h/decombine #{1 2 3 4} even?) => #{1 3}
  (h/decombine #{{:id 1} {:id 2}}
               #(= 1 (:id %)))
  => #{{:id 2}})

(fact "merges"
  (h/merges {:a 1} {:b 1}) => {:a 1 :b 1}
  (h/merges {:a 1} {:a 1}) => {:a 1}
  (h/merges {:a 1} {:a 2}) => {:a #{1 2}}
  (h/merges {:a {:id 1 :val 1}}
            {:a {:id 1 :val 2}})
  => {:a #{{:id 1 :val 1} {:id 1 :val 2}}}

  (h/merges {:a {:id 1 :val 1}}
            {:a {:id 1 :val 2}} :id merge)
  => {:a {:id 1 :val 2}}

  (h/merges {:a {:id 1 :val 1}}
            {:a {:id 1 :val 2}} :id h/merges)
  => {:a {:id 1 :val #{1 2}}}

  (h/merges {:a #{{:id 1}}}
            {:a #{{:id 1 :val 1}}}
            :id merge)
  => {:a #{{:id 1 :val 1}}}

  (h/merges {:a #{{:id 1 :val 1}}}
            {:a #{{:id 1 :val 2}}}
            :id merge)
  => {:a #{{:id 1 :val 2}}}

  (h/merges {:a #{{:id 1 :val 1}}}
            {:a #{{:id 1 :val 2}}}
            :id h/merges)
  => {:a #{{:id 1 :val #{1 2}}}}

  (h/merges {:a #{{:id 1 :val 1}}}
            {:a {:id 1 :val 2}}
            :id h/merges)
  => {:a #{{:id 1 :val #{1 2}}}}

  (h/merges {:a {:foo {:bar {:baz 1}}}}
            {:a {:foo {:bar {:baz 2}}}}
            hash-map?
            (fn [m1 m2] (h/merges m1 m2 hash-map?
                                 (fn [m1 m2] (h/merges m1 m2 hash-map?
                                                      h/merges)))))
  => {:a {:foo {:bar {:baz #{1 2}}}}}

  (h/merges {:a #{{:foo #{{:bar #{{:baz 1}}}}}}}
            {:a #{{:foo #{{:bar #{{:baz 2}}}}}}}
            hash-map?
            (fn [m1 m2] (h/merges m1 m2 hash-map?
                                 (fn [m1 m2] (h/merges m1 m2 hash-map?
                                                      h/merges)))))
  => {:a #{{:foo #{{:bar #{{:baz #{1 2}}}}}}}}

  (h/merges {:a #{{:foo #{{:bar #{{:baz 1}}}}}}}
            {:a {:foo {:bar {:baz 2}}}}
            hash-map?
            (fn [m1 m2] (h/merges m1 m2 hash-map?
                                 (fn [m1 m2] (h/merges m1 m2 hash-map?
                                                      h/merges)))))
  => {:a #{{:foo #{{:bar #{{:baz #{1 2}}}}}}}}

  (h/merges {:a {:id 3 :foo {:bar :A}}}
            {:a {:id 3 :foo {:bar :B}}}
            :id
            (fn [m1 m2] (h/merges m1 m2)))
  => {:a {:id 3 :foo #{{:bar :A} {:bar :B}}}}

  (h/merges {:a {:id 3 :foo {:bar :A}}}
            {:a {:id 3 :foo {:bar :B}}}
            :id
            h/merges)
  => {:a {:id 3 :foo #{{:bar :B} {:bar :A}}}})

(fact "merges-in"
  (h/merges-in {} {:a 1}) => {:a 1}
  (h/merges-in {:a 1} {:a 1}) => {:a 1}
  (h/merges-in {:a 1} {:b 1}) => {:a 1 :b 1}
  (h/merges-in {:a 1} {:a 2}) => {:a #{1 2}}
  (h/merges-in {:a {:b 1}} {:a {:b 1}}) => {:a {:b 1}}
  (h/merges-in {:a {:b 1}} {:a {:b 2}}) => {:a {:b #{1 2}}}
  (h/merges-in {:a 1} {:a {:b 2}}) => {:a #{1 {:b 2}}}
  (h/merges-in {:a {}} {:a {:b 2}}) => {:a {:b 2}}
  (h/merges-in {:a #{{:id 1} {:id 2}}}
               {:a #{{:id 1 :val 1} {:id 2 :val 2}}})
  => {:a #{ {:id 1} {:id 2} {:id 1 :val 1} {:id 2 :val 2}}}
  (h/merges-in {:a #{{:id 1} {:id 2}}}
               {:a #{{:id 1 :val 1} {:id 2 :val 2}}}
               :id merge)
  => {:a #{{:val 1, :id 1} {:val 2, :id 2}}}
  (h/merges-in {:a #{{:foo #{{:bar #{{:baz 1}}}}}}}
               {:a #{{:foo #{{:bar #{{:baz 2}}}}}}}
               hash-map?
               h/merges-in)
  => {:a #{{:foo #{{:bar #{{:baz 2}}}
                   {:bar #{{:baz 1}}}}}}}

  (h/merges-in {:a #{{:foo #{{:bar #{{:baz 1}}}}}}}
               {:a #{{:foo #{{:bar #{{:baz 2}}}}}}}
               hash-map?
               (fn [m1 m2] (h/merges-in m1 m2 hash-map?
                                       (fn [m1 m2] (h/merges-in m1 m2 hash-map?
                                                               h/merges-in)))))
  => {:a #{{:foo #{{:bar #{{:baz #{1 2}}}}}}}}

  (h/merges-in {:a #{1}} {:a 2}
               number?
               +)
  => {:a #{3}})

(fact "merges-in*"
  (h/merges-in* {:a 1} {:a 2} hash-map?)
  => {:a #{1 2}}

  (h/merges-in* {:a #{{:id 1 :foo #{{:id 2 :bar #{{:id 3 :baz 1}}}}}}}
                {:a #{{:id 1 :foo #{{:id 2 :bar #{{:id 3 :baz 2}}}}}}}
                :id)
  => {:a #{{:id 1 :foo #{{:id 2 :bar #{{:id 3 :baz #{1 2}}}}}}}}

  (h/merges-in* {:a #{{:id 1 :foo #{{:id 2 :bar #{{:id 3 :baz 1}}}}}}}
                {:a {:id 1 :foo {:id 2 :bar {:id 3 :baz 2}}}}
                :id)
  => {:a #{{:id 1 :foo #{{:id 2 :bar #{{:id 3 :baz #{1 2}}}}}}}}

  (h/merges-in* {:a {:id 1 :foo {:id 2 :bar {:id 3 :baz 2}}}}
                {:a #{{:id 1 :foo #{{:id 2 :bar #{{:id 3 :baz 1}}}}}}}
                :id)
  => {:a #{{:id 1 :foo #{{:id 2 :bar #{{:id 3 :baz #{1 2}}}}}}}}

  (h/merges-in* {:a #{{:id 1 :foo #{{:id 2 :bar #{{:id 3 :baz 1}}}}}}}
                {:a #{{:id 1 :foo #{{:id 2 :bar #{{:id 3 :baz 2}}}}}}}
                :id +)
  {:a #{{:id 2 :foo #{{:id 4 :bar #{{:id 6 :baz 3}}}}}}}

  (h/merges-in* {:a {:id 1 :b 1}} {:a {:b 2}})
  => {:a {:id 1 :b #{1 2}}}
  (h/merges-in* {:a {:id 1 :b 1}} {:a #{{:b 2}}} identity)
  => {:a #{{:id 1 :b 1} {:b 2}}}
  (h/merges-in* {:a {:id 1 :b 1}} {:a #{{:b 2}}})
  => {:a #{{:id 1 :b #{1 2}}}}
  (h/merges-in* {:a {:id 1 :b 1}} {:a #{{:id 1 :b 2}}})
  => {:a #{{:id 1 :b #{1 2}}}}
  (h/merges-in* {:a {:id 1 :b 1}} {:a #{{:id 2 :b 2}}})
  => {:a #{{:b #{1 2}, :id #{1 2}}}}
  (h/merges-in* {:a {:id 1 :b 1}} {:a #{{:id 2 :b 2}}} identity)
  => {:a #{{:id 1 :b 1} {:id 2 :b 2}}}
  (h/merges-in* {:a {:id 1 :b 1}} {:a #{{:id 2 :b 2}}} :id)
  => {:a #{{:id 1 :b 1} {:id 2 :b 2}}}

  (h/merges-in* {:a {:db {:id 1} :tags "hello" :u1 1}}
                {:a {:db {:id 1} :tags "stuff" :u2 2}} #(-> % :db :id))
  => {:a {:db {:id 1} :tags #{"hello" "stuff"} :u1 1 :u2 2}}
  (h/merges-in* {:a {:db {:id 1} :tags "hello" :u1 1}}
                {:a {:db {:id 1} :tags "stuff" :u2 2}} [:db :id])
  => {:a {:db {:id 1} :tags #{"hello" "stuff"} :u1 1 :u2 2}})

(fact "assocs will merge values into a set when added"
  (h/assocs {} :b 1) => {:b 1}
  (h/assocs {:a 1} :b 1) => {:a 1 :b 1}
  (h/assocs {:a 1} :a 1) => {:a 1}
  (h/assocs {:a 1} :a 2) => {:a #{1 2}}
  (h/assocs {:a #{1}} :a 2) => {:a #{1 2}}
  (h/assocs {:a 1} :a #{2}) => {:a #{1 2}})

(fact "assocs will also do more complicated merges"
  (h/assocs {:a #{1}} :a #{2 3 4}) => {:a #{1 2 3 4}}
  (h/assocs {:a 1} :a 2 number? +) => {:a 3}
  (h/assocs {:a #{1}} :a #{2 3 4} number? +) => {:a #{10}}
  (h/assocs {:a {:id 1}} :a {:id 1 :val 1} :id merge)
  => {:a {:val 1, :id 1}}

  (h/assocs {:a {:id 1}} :a {:id 2} :id merge)
  => {:a #{{:id 1} {:id 2}}}

  (h/assocs {:a #{{:id 1 :val 2}
                  {:id 1 :val 3}}} :a nil :id h/merges)
  => {:a #{{:id 1 :val #{2 3}}}}

  (h/assocs {:a #{{:id 1 :val 2}
                  {:id 1 :val 3}}} :a {:id 1 :val 4} :id h/merges)
  => {:a #{{:id 1 :val #{2 3 4}}}})

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
  (h/dissocs {:a #{1 2}} [:a #{1 2 3}]) => {}
  (h/dissocs {:a #{1 2}} [:a even?]) => {:a #{1}}
  (h/dissocs {:a #{{:id 1} {:id 2}}} [:a #(= 1 (:id %))]) => {:a #{{:id 2}}}
  (h/dissocs {:a 1 :b 2} :b) => {:a 1})


(fact "gets"
  (h/gets {:a 1} :a) => 1
  (h/gets {:a #{1}} :a) => #{1}
  (h/gets {:a #{0 1}} [:a zero?]) => #{0}
  (h/gets {:a #{{:b 1} {}}} [:a :b]) => #{{:b 1}})

(fact "gets-in"
  (h/gets-in {:a 1} [:a]) => #{1}
  (h/gets-in {:a 1} [:b]) => #{}
  (h/gets-in {:a #{{:b 1} {:b 2}}} [:a :b]) => #{1 2})

(fact "assocs-in simple"
  (h/assocs-in {} [:a :b :c] 1)
  => {:a {:b {:c 1}}})

(fact "assocs-in-keyword"
  (h/assocs-in {:a 1 :val 1} [:val] 2)
  => {:a 1, :val #{1 2}}
  (h/assocs-in {:a {:b {:c 1}}} [:a :b :c] 2)
  => {:a {:b {:c #{1 2}}}}
  (h/assocs-in {:a {:b {:c #{1 2}}}} [:a :b :c] 3)
  => {:a {:b {:c #{1 2 3}}}}
  (h/assocs-in {:a 1 :v1 {:b 2 :v2 {:c 3}}} [:v1 :v2 :v3] 3)
  => {:a 1 :v1 {:b 2 :v2 {:c 3 :v3 3}}})

(fact "assocs-in-filtered"
  (h/assocs-in-filtered {:a 1 :val 1} [[:val [:id 1]]] 2)
  => (throws Exception)
  (h/assocs-in-filtered {:a 1 :val #{1}} [[:val [:id 1]]] 2)
  => (throws Exception)
  (h/assocs-in-filtered {:a 1 :v1 #{{:id 1}}} [[:v1 [:id 1]] :v2] 2)
  => {:a 1, :v1 #{{:id 1 :v2 2}}}
  (h/assocs-in-filtered {:a 1 :v1 #{{:id 2}}} [[:v1 [:id 1]] :v2] 2)
  => {:a 1 :v1 #{{:id 2}}}
  (h/assocs-in-filtered {:a 1 :v1 #{{:id 1}{:id 2}}}
                        [[:v1 [:id 1]] :v2 :v3] 3)
  => {:a 1 :v1 #{{:id 1 :v2 {:v3 3}} {:id 2}}}
  (h/assocs-in-filtered {:b {:id 1}} [[:b #(= (:id %) 1)] :c] 2)
  => {:b {:c 2, :id 1}}

  )

(fact "assocs-in"
  (h/assocs-in {} [:a] 1) => {:a 1}
  (h/assocs-in {:a 1} [:a] 2) => {:a #{1 2}}
  (h/assocs-in {:a 1} [:a :b] 2) => (throws Exception)
  (h/assocs-in {:a {:b 1}} [:a :b] 2) => {:a {:b #{1 2}}}
  (h/assocs-in {:a #{{:b 1}}} [:a :b] 2) => {:a #{{:b #{1 2}}}}
  (h/assocs-in {:a {:b {:id 1}}} [:a [:b #(= (:id %) 1)] :c] 2)
  => {:a {:b {:id 1 :c 2}}}
  (h/assocs-in {:a {:b {:id 1}}} [:a [:b [:id 1]] :c] 2)
  => {:a {:b {:id 1 :c 2}}}
  (h/assocs-in {:a #{{:b {:id 1}}
                     {:b {:id 2}}}}
               [:a [:b [:id 1]] :c] 2)
  => {:a #{{:b {:id 1 :c 2}}
           {:b {:id 2}}}}
  (h/assocs-in {:a {:b {:id 1}}} [:a [:b [:id #(= % 1)]] :c] 2)
  => {:a {:b {:id 1 :c 2}}})

(fact "dissocs-in"
  (h/dissocs-in {:a #{{:b 1 :c 1} {:b 2 :c 2}}} [:a :b])
  => {:a #{{:c 1} {:c 2}}}

  (h/dissocs-in {:a #{{:b #{1 2 3} :c 1} {:b #{1 2 3} :c 2}}} [[:a [:c 1]] [:b 1]])
  => {:a #{{:b #{2 3} :c 1} {:b #{1 2 3} :c 2}}})
