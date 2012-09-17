# hara

Hara is a core set of libraries I find that I'm using in alot of my projects. Its currently really small but hopefully will get bigger as I find more patterns that can be abstracted.



## Usage

The main 'usage' is a data structure supporting state-based manipulation of records in the form of maps. Its a useful structure to have as a global object. I use it as a general purpose store that acts as the intermediary between the database and the web presentation layer when there is a need for data to be stored in memory and acted upon in some way. 

In the namespace hara.data.dyna

// Constructor:
(def arec (hara.data.dyna/new))
(def brec (hara.data.dyna/new [{:id :1 :contents "1"} 
                               {:id :2 :contents "2"} 
                               {:id :3 :contents "3"}]))

The data has to be in the form of a map and has to at least contain a field named 'id' additional required keys can be set on initiatialization or automatically inferred from the data. There is an expectation that data has to be in a minimal format for basic error checking.







## License

Copyright © 2012 FIXME

Distributed under the Eclipse Public License, the same as Clojure.
