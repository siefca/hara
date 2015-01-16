(ns vinyasa.reflection
  (:require [hara.reflect :as reflect]
            [hara.reflect.pretty.args :as args]))

(defmacro .%
  "Lists class information
  (.% String)
  => (contains {:name \"java.lang.String\"
                :tag :class
                :hash anything
                :container nil
                :modifiers #{:instance :class :public :final}
                :static false
                :delegate java.lang.String})"
  {:added "2.1"}
  [obj]
  `(reflect/class-info ~obj))

(defmacro .%>
  "Lists the class and interface hierarchy for the class

  (.%> String)
  => [java.lang.String
      [java.lang.Object
       #{java.io.Serializable
         java.lang.Comparable
         java.lang.CharSequence}]]"
  {:added "2.1"}
  [obj]
  `(reflect/class-hierarchy ~obj))

(defmacro .?
  "queries the java view of the class declaration

  (.? String  #\"^c\" :name)
  => [\"charAt\" \"checkBounds\" \"codePointAt\" \"codePointBefore\"
      \"codePointCount\" \"compareTo\" \"compareToIgnoreCase\"
      \"concat\" \"contains\" \"contentEquals\" \"copyValueOf\"]"
  {:added "2.1"}
  [obj & selectors]
  `(reflect/query-class ~obj ~(args/args-convert selectors)))

(defmacro .*
  "lists what methods could be applied to a particular instance

  (.* \"abc\" :name #\"^to\")
  => [\"toCharArray\" \"toLowerCase\" \"toString\" \"toUpperCase\"]

  (.* String :name #\"^to\")
  => (contains [\"toString\"])"
  {:added "2.1"}
  [obj & selectors]
  `(reflect/query-instance ~obj ~(args/args-convert selectors)))

(defmacro .&
  "Allow transparent field access and manipulation to the underlying object.

  (let [a   \"hello\"
        >a  (.& a)]

    (keys >a) => (contains [:hash :value] :in-any-order)

    (seq (>a :value)) => [\\h \\e \\l \\l \\o]

    (>a :value (char-array \"world\"))
    a => \"world\")"
  {:added "2.1"}
  [obj]
  `(reflect/delegate ~obj))

(defmacro .>var
  "extracts a class method into a namespace.

  (.>var hash-without [clojure.lang.IPersistentMap without])

  (with-out-str (eval '(clojure.repl/doc hash-without)))
  => (str \"-------------------------\\n\"
          \"vinyasa.reflection-test/hash-without\\n\"
          \"[[clojure.lang.IPersistentMap java.lang.Object]]\\n\"
          \"  \\n\"
          \"member: clojure.lang.IPersistentMap/without\\n\"
          \"type: clojure.lang.IPersistentMap\\n\"
          \"modifiers: instance, method, public, abstract\\n\")

  (eval '(hash-without {:a 1 :b 2} :a))
  => {:b 2}"
  {:added "2.1"}
  ([name [class method & selectors]]
   `(reflect/extract-to-var ~(list `symbol (str name)) ~class ~(str method) ~(args/args-convert selectors)))
  ([name objvec & more]
   [`(.>var ~name ~objvec)]
    ~@(map #(cons `.>var %) (partition 2 more))))

(defmacro .>ns
  "extracts all class methods into its own namespace.

  (map #(.sym %)
       (.>ns test.string String :private #\"serial\"))
  => '[serialPersistentFields serialVersionUID]"
  {:added "2.1"}
  [ns class & selectors]
  `(reflect/extract-to-ns ~(list `symbol (str ns)) ~class ~(args/args-convert selectors)))

(defmacro .>
  "Threads the first input into the rest of the functions. Same as `->` but
   allows access to private fields using both `:keyword` and `.symbol` lookup:

  (.> \"abcd\" :value String.) => \"abcd\"

  (.> \"abcd\" .value String.) => \"abcd\"

  (let [a  \"hello\"
        _  (.> a (.value (char-array \"world\")))]
    a)
  => \"world\""
  {:added "2.1"}
  ([obj] obj)
  ([obj method]
     (cond (not (list? method))
           `(.> ~obj (~method))

           (list? method)
           (let [[method & args] method]
             (cond (#{'.* '.? '.% '.%> '.&} method)
                   `(~(symbol (str "vinyasa.reflection/" method)) ~obj ~@args)

                   (and (symbol? method) (.startsWith (name method) "."))
                   `(reflect/apply-element ~obj ~(subs (name method) 1) ~(vec args))

                   (keyword? method)
                   `(or (~method ~obj ~@args)
                        (let [nm# ~(subs (str method) 1)]
                          (if (some #(= % nm#) (reflect/query-instance ~obj [:name]))
                            (reflect/apply-element ~obj nm# ~(vec args)))))

                   :else
                   `(~method ~obj ~@args)
                   ))))

  ([obj method & more]
     `(.> (.> ~obj ~method) ~@more)))
