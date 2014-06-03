(ns hara.namespace.import
  (:refer-clojure :exclude [import]))

(defn import-var
  "Imports a single var from one namespace to the current one.

  (import-var 'ifl #'clojure.core/if-let)
  =>  #'hara.namespace.import-test/ifl
  (eval '(ifl [a 1] (inc a))) => 2
  
  "
  {:added "2.0"}
  [name ^clojure.lang.Var var]
  (if (.hasRoot var)
    (intern *ns* (with-meta name (merge (meta var)
                                        (meta name)))
            @var)))

(defn import-namespace
  "Imports all or a selection of vars from one namespace to the current one.

  (import-namespace 'hara.common.checks '[bytes? long?])
  (eval '(long? 1))  => true
  (eval '(bytes? 1)) => false
  
  "
  {:added "2.0"}
  ([ns] (import-namespace ns nil))
  ([ns vars]
     (let [all-vars (ns-publics ns)
           selected-vars (if vars
                            (select-keys all-vars vars)
                            all-vars)]
       (doseq [[n v] selected-vars]
         (import-var n v)))))

(defmacro import
  "Imports all or a selection of vars from one namespace to the current one.

  (import hara.common.checks [bytes? long?]) => nil
  (eval '(long? 1))  => true
  (eval '(bytes? 1)) => false

  (import hara.common.checks :all) => nil
  (eval '(bigint? 1)) => false

  "
  {:added "2.0"}
  [nsp vars & more]
  `(do
     (require (quote ~nsp))
     (import-namespace
      (quote ~nsp)
      ~(if-not (= :all vars)
         `(quote ~vars)))
     ~(if more
        `(import ~@more))))
