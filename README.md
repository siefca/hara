# hara

Hara is a core set of libraries I find that I'm using in alot of my projects. Its currently really small but hopefully will get bigger as I find more patterns that can be abstracted.

### Installation:

In project.clj, add to dependencies:

     [hara "0.4.1"]


## Usage

There is `hara.iotam` which is a play on clojure's atoms. It is an atom with a more powerful 'watch' function. The watch function for atoms accept four parameters: a key, the ref, the previous value and the changed value. `iotam`s accept an additional parameters: the type of operation (:reset or :swap), and if it is a swap, the function as well as the arguments to the function. Call it an atom that can be watched with more awareness.

    (use '[hara.iotam :only [iotam ireset! iswap!]])
    (def ai (iotam 9))

    (add-watch ai :print println)
    (iswap! ai inc)
    (ireset! ai 0)

There is `hara.dyna`, the main 'usage' is a data structure supporting state-based manipulation of records in the form of maps. Its a useful structure to have as a global object. I use it as a general purpose store that acts as the intermediary between the database and the web presentation layer when there is a need for data to be stored in memory and acted upon in some way. The dyna can also be watched as it is built using `iotam`s.

    // Constructor:
    (def arec (hara.dyna/dyna))
    (def brec (hara.dyna/dyna [{:id :1 :contents "1"}
                                   {:id :2 :contents "2"}
                                   {:id :3 :contents "3"}]))


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
