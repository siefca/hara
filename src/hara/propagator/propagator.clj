(comment (ns hara.propagator.propagator
           (:gen-class
            :name hara.propagator.Propagator
            :prefix "-"
            :init init
            :constructors {[] []}
            :state state
            :extends clojure.lang.AFn
            :methods [[propagate [] void]
                      [setTfn [clojure.lang.IFn] void]
                      [getTfn [] clojure.lang.IFn]
                      [setTdamp [clojure.lang.IFn] void]
                      [getTdamp [] clojure.lang.IFn]
                      [setInputs [clojure.lang.PersistentVector] void]
                      [getInputs [] clojure.lang.PersistentVector]
                      [setOutput [hara.propagator.Cell] void]
                      [getOutput [] hara.propagator.Cell]]
            :import [hara.propagator.Cell]))

         (def )

         (defn -init
           ([] [[]  {:inputs (atom [])
                     :output (atom :hara.propagator.cell/nothing)
                     :t-fn (atom (fn [& [x]] x))
                     :t-damp (atom =)}
                ]))

         (defn -propagate [pg])

         (defn -setTFn [pg f])
         (defn -getTFn [pg])
         (defn -setTDamp )
         (defn -getTDamp )
         (defn -setInputs )

         (defrecord Example [data] clojure.lang.IFn
                    (invoke [this] this)
                    (invoke [this n] (repeat n this))
                    (applyTo [this args] (clojure.lang.AFn/applyToHelper this args))))
