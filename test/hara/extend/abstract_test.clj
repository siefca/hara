(ns hara.extend.abstract-test
  (:use midje.sweet)
  (:require [hara.extend.abstract :refer :all]))

(defprotocol IData (-data [this]))
(defrecord Envelope [])

^{:refer hara.extend.abstract/protocol-basis :added "2.1"}
(fact "Helper function that transforms the functions in the protocol to
  the neccessary format in preparation for extend-abstract and extend-implementations"

  (defprotocol IVal
    (-set [this val])
    (-get [this]))

  (protocol-basis IVal '- 'pre- '-tail)
  => '({:args [this], :fn pre-get-tail, :name -get}
       {:args [this val], :fn pre-set-tail, :name -set}))

^{:refer hara.extend.abstract/map-walk-submap :added "2.1"}
(fact "Gets a submap depending on whether it is a key or it
  is a key witthin a hashset"

  (map-walk-submap {:hello "world"} :hello)
  => "world"

  (map-walk-submap {#{:hello} "world"} :hello)
  => "world")

^{:refer hara.extend.abstract/map-walk :added "2.1"}
(fact "Helper function for evaluation of various utility functions
  within the namespace"

  (map-walk :hello {#{:hello} (fn [k arg1] (str (name k) " world " arg1))}
            ["again"]  identity
            (fn [_ _ _] :none)
            (fn [obj func arg1] (func obj arg1)))
  => "hello world again")

^{:refer hara.extend.abstract/protocol-default-form :added "2.1"}
(fact "creates a :default defmethod form from a protocol basis"

  (protocol-default-form '{:args [this], :fn data-env, :name -data}
                         '{#{-data} ([this & args] (Exception. "No input"))})
  => '(defmethod data-env :default [this & args] (Exception. "No input")))

^{:refer hara.extend.abstract/protocol-multi-form :added "2.1"}
(fact "creates a :default defmethod form from a protocol basis"

  (protocol-multi-form '{:args [this], :fn data-env, :name -data}
                       '{#{-data} (-> % :meta :type)})
  => '(defmulti data-env (fn [this] (-> this :meta :type))))

^{:refer hara.extend.abstract/protocol-multimethods :added "2.1"}
(fact "creates a set of defmulti and defmethods for each entry in all-basis"

  (protocol-multimethods '[{:args [this], :fn data-env, :name -data}]
                         {:defaults '([this & args] (Exception. "No input"))
                          :dispatch '(-> % :meta :type)})
  => '((defmulti data-env (fn [this] (-> this :meta :type)))
       (defmethod data-env :default [this & args] (Exception. "No input"))))

^{:refer hara.extend.abstract/protocol-extend-type-template :added "2.1"}
(fact "applies form template for simple template rewrites"

  (protocol-extend-type-template '{:args [this], :fn data-env, :name -data}
                                 '{-data (process %)}
                                 '(data-env this))
  => '(process (data-env this)))

^{:refer hara.extend.abstract/protocol-extend-type-macro :added "2.1"}
(fact "applies a macro for simple template rewrites"

  (protocol-extend-type-macro '{:args [this], :fn data-env, :name -data}
                              '{-data (fn [form basis] (concat ['apply] form [[]]))}
                              '(data-env this))
  => '(apply data-env this []))

^{:refer hara.extend.abstract/protocol-extend-type-function :added "2.1"}
(fact "utility to create a extend-type function  with template and macros"

  (protocol-extend-type-function '{:args [this], :fn data-env, :name -data}
                                 '{-data (process %)}
                                 '{-data (fn [form basis] (concat ['apply] form [[]]))})
  => '(-data [this] (apply process (data-env this) [])))

^{:refer hara.extend.abstract/protocol-extend-type :added "2.1"}
(fact "utility to create an extend-type form"
  (protocol-extend-type 'Type 'IProtocol
                        '[{:args [this], :fn data-env, :name -data}]
                        '{:template (process %)
                          :macro (fn [form basis] (concat ['apply] form [[]]))})
  => '(extend-type Type IProtocol
                   (-data [this] (apply process (data-env this) []))))

^{:refer hara.extend.abstract/extend-abstract :added "2.1"}
(fact "Creates a set of abstract multimethods as well as extends a set of
  protocols to a given type"

  (extend-abstract
   Envelope [IData]
   :select -
   :suffix -env
   :prefix nil
   :template   {-data  (str "hello " %)}
   :dispatch   :type
   :defaults   {nil   ([this & args] (Exception. "No input"))
                -data ([this] (:hello this))})

  (data-env (map->Envelope {:hello "world"}))
  => "world"

  (-data (map->Envelope {:hello "world"}))
  => "hello world")

^{:refer hara.extend.abstract/extend-implementations :added "2.1"}
(fact "Creates a set of implementation functions for implementation
  of protocol functionality"

  (extend-implementations
   [IData]
   :macro (fn [form _]
            (list 'str form " again")))

  (data (map->Envelope {:hello "world"}))
  => "hello world again")
