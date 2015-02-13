(ns hara.extend.abstract
  (:require [hara.common.checks :refer [hash-map?]]
            [clojure.walk :as walk]))

(defn protocol-basis
  "Helper function that transforms the functions in the protocol to
  the neccessary format in preparation for extend-abstract and extend-implementations

  (defprotocol IVal
    (-set [this val])
    (-get [this]))

  (protocol-basis IVal '- 'pre- '-tail)
  => '({:args [this], :fn pre-get-tail, :name -get}
       {:args [this val], :fn pre-set-tail, :name -set})"
  {:added "2.1"}
  [protocol select prefix suffix]
  (->> protocol :sigs vals
       (filter #(-> % :arglists count (= 1)))
       (filter #(-> % :name str (.startsWith (name select))))
       (map (fn [m]
              (let [nm   (str (:name m))
                    args (-> m :arglists first)
                    prefix (if (nil? prefix) "" (name prefix))
                    suffix (if (nil? suffix) "" (name suffix))]
                (-> m
                    (assoc :fn
                      (-> nm
                          (.replaceFirst (name select) "")
                          (->> (str prefix))
                          (str suffix)
                          symbol))
                    (assoc :args args)
                    (dissoc :arglists :doc)))))))

(defn map-walk-submap
  "Gets a submap depending on whether it is a key or it
  is a key witthin a hashset

  (map-walk-submap {:hello \"world\"} :hello)
  => \"world\"

  (map-walk-submap {#{:hello} \"world\"} :hello)
  => \"world\""
  {:added "2.1"}
  [m search-key]
  (->> m
       (keep (fn [[k v]]
               (if (or (= search-key k)
                       (and (set? k)
                            (get k search-key)))
                 v)))
       first))

(defn map-walk
  "Helper function for evaluation of various utility functions
  within the namespace

  (map-walk :hello {#{:hello} (fn [k arg1] (str (name k) \" world \" arg1))}
            [\"again\"]  identity
            (fn [_ _ _] :none)
            (fn [obj func arg1] (func obj arg1)))
  => \"hello world again\""
  {:added "2.1"}
  [obj mapobj args f-obj f-nil f-nonmap]
  (cond (nil? mapobj) (apply f-nil obj mapobj args)

        (hash-map? mapobj)
        (if-let [submap (map-walk-submap mapobj (f-obj obj))]
          (recur obj submap args f-obj f-nil f-nonmap)
          (recur obj (map-walk-submap mapobj nil) args f-obj f-nil f-nonmap))

        :else
        (apply f-nonmap obj mapobj args)))

(defn protocol-default-form
  "creates a :default defmethod form from a protocol basis

  (protocol-default-form '{:args [this], :fn data-env, :name -data}
                         '{#{-data} ([this & args] nil)})
  => '(defmethod data-env :default [this & args] nil)

  (protocol-default-form '{:args [this], :fn data-env, :name -data}
                         '{#{-data} (fn [basis]
                                      `(~(:args basis)
                                        (throw (Exception. (str ~(str \"No implementation of \" (:fn basis) \" for \")
                                                                (-> ~(-> basis :args first) :meta :type))))))})
  => '(defmethod data-env :default [this]
        (throw (java.lang.Exception.
                (clojure.core/str \"No implementation of data-env for \"
                                  (clojure.core/-> this :meta :type)))))"
  {:added "2.1"}
  [basis defaults]
  (map-walk basis defaults [] :name
            (fn [_ _] nil)
            (fn [basis defaults]
              (let [form (cond (or (symbol? defaults)
                                   (#{'fn 'clojure.core/fn} (first defaults)))
                               ((eval defaults) basis)

                               :else defaults)]
                (concat (list 'defmethod (:fn basis) :default (first form))
                        (rest form))))))

(defn protocol-multi-form
  "creates a :default defmethod form from a protocol basis

  (protocol-multi-form '{:args [this], :fn data-env, :name -data}
                       '{#{-data} (-> % :meta :type)})
  => '(defmulti data-env (fn [this] (-> this :meta :type)))"
  {:added "2.1"}
  [basis dispatch]
  (map-walk basis dispatch [] :name
            (fn [_ _] (protocol-multi-form basis (-> basis :args first)))
            (fn [basis dispatch]
              (list 'defmulti (:fn basis)
              (cond (keyword? dispatch) dispatch

                    :else
                    (list 'fn (:args basis)
                          (let [sym (-> basis :args first)]
                            (walk/prewalk-replace {'% sym} dispatch))))))))

(defn protocol-multimethods
  "creates a set of defmulti and defmethods for each entry in all-basis

  (protocol-multimethods '[{:args [this], :fn data-env, :name -data}]
                         {:defaults '([this & args] (Exception. \"No input\"))
                          :dispatch '(-> % :meta :type)})
  => '((defmulti data-env (fn [this] (-> this :meta :type)))
       (defmethod data-env :default [this & args] (Exception. \"No input\")))"
  {:added "2.1"}
  [all-basis {:keys [defaults dispatch]}]
  (->> all-basis
       (mapcat (juxt #(protocol-multi-form % dispatch)
                     #(protocol-default-form % defaults)))
       (keep identity)))

(defn protocol-extend-type-wrappers
  "applies form template for simple template rewrites

  (protocol-extend-type-wrappers '{:args [this], :fn data-env, :name -data}
                                 '{-data (process %)}
                                 '(data-env this))
  => '(process (data-env this))

  (protocol-extend-type-wrappers '{:args [this], :fn data-env, :name -data}
                                 '{-data (fn [form basis] (concat ['apply] form [[]]))}
                                 '(data-env this))
  => '(apply data-env this [])"
  {:added "2.1"}
  [basis wrappers form]
  (map-walk basis wrappers [form] :name
            (fn [_ _ form] form)
            (fn [basis wrapper form]
              (cond (or (symbol? wrapper)
                        (#{'fn 'clojure.core/fn} (first wrapper)))
                    ((eval wrapper) form basis)

                    :else
                    (walk/prewalk-replace {'% form} wrapper)))))

(defn protocol-extend-type-function
  "utility to create a extend-type function  with template and macros

  (protocol-extend-type-function '{:args [this], :fn data-env, :name -data}
                                 '{-data (fn [form basis] (concat ['apply] form [[]]))})
  => '(-data [this] (apply data-env this []))"
  {:added "2.1"}
  [basis wrappers]
  (list (:name basis) (:args basis)
        (->> (cons (:fn basis) (:args basis))
             (protocol-extend-type-wrappers basis wrappers))))

(defn protocol-extend-type
  "utility to create an extend-type form
  (protocol-extend-type 'Type 'IProtocol
                        '[{:args [this], :fn data-env, :name -data}]
                        '{:wrappers (fn [form basis] (concat ['apply] form [[]]))})
  => '(extend-type Type IProtocol
                   (-data [this] (apply data-env this [])))"
  {:added "2.1"}
  [typesym protocolsym all-basis {:keys [wrappers]}]
  (concat (list 'extend-type typesym protocolsym)
          (map #(protocol-extend-type-function % wrappers)
               all-basis)))

(defn protocol-all [typesym protocolsym {:keys [select prefix suffix] :as options}]
  (let [select (or select '-)
        protocol  (eval protocolsym)
        all-basis (protocol-basis protocol select prefix suffix)]
    (list 'let ['function (vec (protocol-multimethods all-basis options))]
          (protocol-extend-type typesym protocolsym all-basis options)
          'function)))

(defmacro extend-abstract
  "Creates a set of abstract multimethods as well as extends a set of
  protocols to a given type

  (extend-abstract
   Envelope [IData]
   :select -
   :suffix -env
   :prefix nil
   :wrappers   {-data  (str \"hello \" %)}
   :dispatch   :type
   :defaults   {nil   ([this & args] (Exception. \"No input\"))
                -data ([this] (:hello this))})

  (data-env (map->Envelope {:hello \"world\"}))
  => \"world\"

  (-data (map->Envelope {:hello \"world\"}))
  => \"hello world\""
  {:added "2.1"}
  [typesym protocolsyms & {:as options}]
  (list `keep `identity
        (cons `concat
              (map #(protocol-all typesym % options)
                   protocolsyms))))

(defn protocol-implementation-function [basis wrappers pns]
  (list `defn (:fn basis) (:args basis)
        (->> (cons (symbol (str pns "/" (:name basis))) (:args basis))
             (protocol-extend-type-wrappers basis wrappers))))

(defn protocol-ns [protocol]
  (->  ^clojure.lang.Var (protocol :var) .ns str))

(defn protocol-implementation [protocolsym {:keys [select prefix suffix wrappers] :as options}]
  (let [select (or select '-)
        protocol  (eval protocolsym)
        all-basis (protocol-basis protocol select prefix suffix)
        pns (protocol-ns protocol)]
    (map #(protocol-implementation-function % wrappers pns)
         all-basis)))

(defmacro extend-implementations
  "Creates a set of implementation functions for implementation
  of protocol functionality

  (extend-implementations
   [IData]
   :wrappers (fn [form _]
               (list 'str form \" again\")))

  (data (map->Envelope {:hello \"world\"}))
  => \"hello world again\""
  {:added "2.1"}
  [protocolsyms & {:as options}]
  (vec
   (mapcat #(protocol-implementation % options)
           protocolsyms)))
