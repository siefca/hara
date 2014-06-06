(ns hara.class.inheritance
  (:refer-clojure :exclude [list]))

(defn list
  "Lists the direct ancestors of a class
  (inheritance/list clojure.lang.PersistentHashMap)
  => [clojure.lang.PersistentHashMap
      clojure.lang.APersistentMap
      clojure.lang.AFn
      java.lang.Object]"
  {:added "2.1"}
  ([cls] (list cls []))
  ([cls output]
     (if (nil? cls)
       output
       (recur (.getSuperclass cls) (conj output cls)))))

(defn tree
  "Lists the hierarchy of bases and interfaces of a class.
  (inheritance/tree Class)
  => [[java.lang.Object #{java.io.Serializable
                          java.lang.reflect.Type
                          java.lang.reflect.AnnotatedElement
                          java.lang.reflect.GenericDeclaration}]]
  "
  {:added "2.1"}
  ([cls] (tree cls []))
  ([cls output]
     (let [base (.getSuperclass cls)]
       (if-not base output
               (recur base
                      (conj output [base (-> (.getInterfaces cls) seq set)]))))))
