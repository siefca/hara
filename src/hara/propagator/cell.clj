(comment (ns hara.propagator.cell
           (:gen-class
            :name hara.propagator.Cell
            :prefix "-"
            :init init
            :constructors {[] []}
            :state state
            :extends clojure.lang.AFn
            :methods [[reset [] void]
                      [addPropagator [hara.propagator.Propagator] void]
                      [removePropagator [hara.propagator.Propagator] void]
                      [getPropagators [] clojure.lang.PersistentHashSet]
                      [clearPropagators [] void]
                      [notifyPropagators [] void]
                      [getContent [] java.lang.Object]
                      [setContent [java.lang.Object] void]
                      [clearContent [] void]]
            :import [hara.propagator.Propagator]))

         (def nothing ::nothing)

         (defn content [cell] (:content (.state cell)))

         (defn propagators [cell] (:content (.state cell)))

         (defn -init
           ([]     [[]  {:content       (ref nothing)
                         :propagators   (atom #{})}]))

         (defn -getContent
           [cell]
           (deref (content cell)))

         (defn -setContent
           [cell obj]
           (dosync (ref-set (content cell) obj)))

         (defn -getPropagators
           [cell]
           (deref (propagators cell)))

         (defn -clearPropagators
           [cell]
           (reset! (propagators cell) []))

         (defn -addPropagator [cell prg]
           (swap! (propagators cell) conj prg))

         (defn -removePropagator [cell prg]
           (swap! (propagators cell) disj prg))

         (defn -reset [cell]
           (-setContent cell nothing)
           (-clearPropagators cell))

         (defn -notifyPropagators [cell])

         (defn -invoke
           ([cell] (-getContent cell))
           ([cell val]
              (-setContent cell val)
              (-notifyPropagators cell)))


         (comment ))
