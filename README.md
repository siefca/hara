# hara

A set of utilities for working with state based data in multithreaded applications.

Stateful data structures for clojure (and the functions to operate on them)

### Huh?

<i>Isn't the whole point of clojure to move to a more functional style of programming using immutable data structures?</i>

Yes. Exactly. Clojure has rid of alot of complexity on the jvm by requiring the programmer to think functionally. However, the issue of shared state is still a problem in multithreaded applications. Clojure has `ref`s and `atom`s to resolve this issue but they tend to be a little basic.

A typical use case of stored state is an atom with a array containing data:

 - Data can be added or removed from the array
 - The data itself can be also changed
 - The data has to be accessible by a number of threads

In the case above, the best option would be to construct a `ref` containing an array of `ref`s containing data. What hara offers is essentially functions that manipulate this structure (which is given the name `ova`).

### Installation:

In project.clj, add to dependencies:

     [hara "0.6.1"]

## hara.ova

There is `hara.ova`, the main data structure supporting state-based manipulation of records. Its a useful structure to have as a shared state. I use it as a transactional general purpose store that is the intermediary between the main application, the database and the web presentation layer when there is a need for data to be stored in memory and acted upon in some way by multiple threads.

    (require '[hara.ova :as v])

    (def ov (v/ova [{:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 2} {:id 4 :val 2}]))))

#### select

`select` gives many options to filter data:

    (v/select ov)   ;; select all
    ;; => [{:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 2} {:id 4 :val 2}]
    
    (v/select ov 0)  ;; select by index
    ;; => [{:id 1 :val 1}]

    (v/select ov #{1 2}) ;; select using a set of indices
    ;; => [{:id 2 :val 1} {:id 3 :val 2}]
    
    (v/select ov #(odd? (:id %)))  ;; select using a predicate
    ;; => [{:id 1 :val 1} {:id 3 :val 2}]
    
    (v/select ov [:id 1])  ;; select using key/value pairs
    ;; => [{:id 1 :val 1}]
    
    (v/select ov [:val even? :id odd?])  ;; can allow multiple matches on key/value pairs
    ;; => [{:id 3 :val 2}]

    (v/select ov [[:id :val] 5])  ;; also has syntax to allow for nested keys
    ;; => [{:id {:val 5}}]

#### indices

`indices` is like `select` but return the indices instead of the elements

    (v/indices ov #(odd? (:id %))) 
    ;; => [0 2]

    (v/indices ov #(even? (:val %))) 
    ;; => [2 3]
    
    (v/indices ov [:id odd?]) 
    ;; => [0 2]


#### set

`set>` sets the value using the matching function

    (def ov (v/ova [1 2 3 4 5 6]))))
  
    (dosync (v/set> ov 0 :a)) ;; set-val by index 
    (v/select ov)
    ;; => [:a 2 3 4 5 6]

    (dosync (v/set-val ov #{1 2} :a)) ;; set-val by indices
    ;; => [1 :a :a 4 5 6]

    (dosync (v/set-val ov odd? 0)) ;; set-val using predicate
    ;; => [:a 2 :a 4 :a 6]

#### update

`update>` uses the same checking convention as select. it should be used only when data is in the form of hashmaps. it will simultaneously update multiple entries if the checking function returns true for each of them

    (def ov (v/ova [{:id 1 :val 1} {:id 2 :val 2} 0]))))

    (dosync (v/update ov 0 {:val :CHANGED}))
    (v/select ov) 
    ;; => [{:id 1 :val :CHANGED} {:id 2 :val 2} 0]

    (dosync (v/update ov [:val 2] {:id :CHANGED}))
    (v/select ov) 
    ;; => [{:id 1 :val :1} {:id :CHANGED :val 2} 0]

#### insert

`insert` adds to the end or at an index

    (def ov (v/ova [{:id 1 :val 1}]))))

    (dosync (v/insert ov {:id 3 :val 3})) 
    (v/select ov)
    [{:id 1 :val 1} {:id 3 :val 3}]

    (dosync (v/insert ov {:id 2 :val 2} 1)) 
    (v/select ov)
    [{:id 1 :val 1} {:id 2 :val 2} {:id 3 :val 3}]

#### delete

`delete` removes all matching entries

    (def ov (v/ova [{:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 2} {:id 4 :val 2}]))

    (dosync (v/delete ov [:id odd? :val even?]))
    (v/select ov)
    ;; => {:id 1 :val 1} {:id 2 :val 1} {:id 4 :val 2}

#### map and smap

`map` and `smap` provides a general function for manipulating the array

    (def ov (v/ova [1 2 3 4 5 6]))))
  
    (dosync (v/smap ov odd? inc) ;; increment odd numbers 
    (v/select ov)
    ;; => [2 2 4 4 6 6]

    (dosync (v/map ov inc)) ;; increment all values
    (v/select ov)
    ;; => [3 3 5 5 7 7]

#### watches

`add-elem-watch` allows a generic element watch can be placed on the ova, such that when any values change or has been updated, the watch will be executed. the watch function takes 5 arguments: the ova, the ref, the key, the previous value and the next value.

    (def out (atom []))
    (let [ov     (v/ova [1 2 3 4]
        cj-fn  (fn  [_ _ _ p v & args]
                 (swap! out conj [p v]))
        _      (v/add-elem-watch ov :conj cj-fn)
        _      (dosync (v/delete ov 0))]
        (dosync (v/map ov inc)))))

    (sort @out) 
    ;; => [[2 3] [3 4] [4 5]]

#### other functions

tried to be as clojurish as possible:

    - dissoc
    - update-in
    - set-in
    - sort
    - filter
    - reverse
    - MORE ADDED AS NEEDED


    ## hara.fn/manipulate

    manipulate is a higher order function for manipulating entire data trees. 
    it is useful for type conversion for serialization/deserialization

        (require '[hara.fn :as f])

        (f/manipulate* (fn [x] (* 2
                                   (cond (string? x) (Integer/parseInt x)
                                         :else x)))
                         {1 "2" 3 ["4" 5 #{6 "7"}]})
        ;; => {2 4 6 [8 10 #{12 14}]

        (f/manipulate* (fn [x] (* 2 x))
                     {1 "2" 3 ["4" 5 #{6 "7"}]}
                     [{:pred String
                       :dtor (fn [x] (Integer/parseInt x))
                       :ctor (fn [x] [(.toString x)])}])
        ;; => {2 ["4"] 6 [["8"] 10 #{12 ["14"]}]

        (f/manipulate* identity
                     [1 [:date 2 3 4 5] 6 7]
                     [{:pred #(and (vector? %) (= (first %) :date))
                       :dtor #(apply t/date-time (rest %))}])
        ;; => [1 (t/date-time 2 3 4 5) 6 7])

        (f/manipulate* identity
                     [1 (t/date-time 2 3 4 5) 6 7]
                     [{:pred org.joda.time.DateTime
                       :dtor (fn [dt] [:date (t/year dt) (t/month dt)])}])
        ;; => [1 [:date 2 3] 6 7])



## TODOS:

- Fix up the readme to include more examples:


## License
Copyright © 2013 Chris Zheng

Distributed under the MIT License
