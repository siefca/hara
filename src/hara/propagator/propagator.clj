(ns hara.propagator.propagator
  (:gen-class
   :name hara.propagator.Propagator
   :prefix "-"
   :init init
   :constructors {[] []}
   :state state
   :extends clojure.lang.AFn
   :methods []))

   (defn -init
     ([]     [[]  {}]))