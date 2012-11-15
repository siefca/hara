# hara

Stateful data structures for clojure.

### Huh?

<i>Isn't the whole point of clojure to move to a more functional style of programming using immutable data structures?</i>

Yes. Exactly. Clojure has rid of alot of complexity on the jvm by requiring the programmer to think functionally. However, the issue of shared state is still a problem in multithreaded applications. Clojure has `ref`s and `atom`s to resolve this issue but they tend to be a little basic.

A typical use case of stored state is an atom with a array containing data: 
 - Data can be added or removed from the array
 - The data itself can be also changed
 - The data has to be accessible by a number of threads

In the case above, the best option would be to construct an atom containing an array of atoms containing data. What hara offers is essentially functions that manipulate this structure (which is given the name 'eva').

### Installation:

In project.clj, add to dependencies:

     [hara "0.6.1"]


## Evom

There is `hara.data.evom` which is a play on clojure's atoms. It is an atom that accepts a more powerful 'watch' function. The watch function for atoms accept four parameters: a key, the ref, the previous value and the changed value. `evom`s accept an additional parameters: the type of operation (:reset or :swap), and if it is a swap, the function as well as the arguments to the function. Call it an atom that can be watched with more awareness.

    (use '[hara.evom :only [evom reset! swap!]])
    (def ai (evom 9))

    (add-watch ai :print println) ;; installs the watch
    (swap! ai inc)
    ;; => :print #<Evom@1b01ade9: 10> 9 10 :swap #<core$inc clojure.core$inc@26b77801> ()

    (reset! ai 0)
    ;; => :print #<Evom@1b01ade9: 0> 10 0 :reset nil nil

## Eva

There is `hara.eva`, the main data structure supporting state-based manipulation of records. Its a useful structure to have as a shared state. I use it as a general purpose store that acts as the intermediary between the main application, the database and the web presentation layer when there is a need for data to be stored in memory and acted upon in some way. The eva can also be watched as it is built using `evom`s.

    // Constructor:
    (require '[hara.eva :as v])
    (def ev (v/eva [{:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 2} {:id 4 :val 2}]))

### Selection
    (v/select ev) 
    ;; => [{:id 1 :val 1} {:id 2 :val 1} {:id 3 :val 2} {:id 4 :val 2}]
    
    (v/select ev 0)          ;; selection using indices
    ;; => [{:id 1 :val 1}]
    (v/select ev 2) 
    ;; => [{:id 3 :val 2}] 
    (v/select ev #{4}) 
    ;; => []
    (v/select ev #{1 2}) 
    ;; => [{:id 2 :val 1} {:id 3 :val 2}]

    (v/select ev [:id 1])    ;; selection using key matches 
    ;; => [{:id 1 :val 1}]
    (v/select ev [:val 1]) 
    ;; => [{:id 1 :val 1} {:id 2 :val 1}] 
    (v/select ev [:id odd?]) 
    ;; => [{:id 1 :val 1} {:id 3 :val 2}]

    (v/select ev #(odd? (:id %))) => [{:id 1 :val 1} {:id 3 :val 2}]
    (v/select ev #(even? (:val %))) => [{:id 3 :val 2} {:id 4 :val 2}]


The data has to be in the form of a map and has to at least contain a field named 'id' additional required keys can be set on initiatialization or automatically inferred from the data. There is an expectation that data has to be in this minimal format for basic error checking.


## TODOS:

- Fix up the readme to include more examples:
  - load/save from korma
  - search - DONE
  - update based upon a predicate - DONE

- Do unit tests for hara.data.dyna-rec - DONE



## License

Copyright © 2012 Chris Zheng

Distributed under the Eclipse Public License, the same as Clojure.
