(ns vinyasa.reflection-test
  (:require [midje.sweet :refer :all]
            [vinyasa.reflection :refer :all]))

(refer-clojure :exclude '[.% .%> .? .* .& .> .>ns .>var])

^{:refer vinyasa.reflection/.% :added "2.1"}
(fact "Lists class information"
  (.% String)
  => (contains {:name "java.lang.String"
                :tag :class
                :hash anything
                :container nil
                :modifiers #{:instance :class :public :final}
                :static false
                :delegate java.lang.String}))

^{:refer vinyasa.reflection/.%> :added "2.1"}
(fact "Lists the class and interface hierarchy for the class"

  (.%> String)
  => [java.lang.String
      [java.lang.Object
       #{java.io.Serializable
         java.lang.Comparable
         java.lang.CharSequence}]])

^{:refer vinyasa.reflection/.& :added "2.1"}
(fact "Allow transparent field access and manipulation to the underlying object."

  (let [a   "hello"
        >a  (.& a)]

    (keys >a) => (contains [:hash :value] :in-any-order)

    (seq (>a :value)) => [\h \e \l \l \o]

    (>a :value (char-array "world"))
    a => "world"))

^{:refer vinyasa.reflection/.? :added "2.1"}
(fact "queries the java view of the class declaration"

  (.? String  #"^c" :name)
  => ["charAt" "checkBounds" "codePointAt" "codePointBefore"
      "codePointCount" "compareTo" "compareToIgnoreCase"
      "concat" "contains" "contentEquals" "copyValueOf"])

^{:refer vinyasa.reflection/.* :added "2.1"}
(fact "lists what methods could be applied to a particular instance"

  (.* "abc" :name #"^to")
  => ["toCharArray" "toLowerCase" "toString" "toUpperCase"]

  (.* String :name #"^to")
  => (contains ["toString"]))

^{:refer vinyasa.reflection/.> :added "2.1"}
(fact "Threads the first input into the rest of the functions. Same as `->` but
   allows access to private fields using both `:keyword` and `.symbol` lookup:"

  (.> "abcd" :value String.) => "abcd"

  (.> "abcd" .value String.) => "abcd"

  (let [a  "hello"
        _  (.> a (.value (char-array "world")))]
    a)
  => "world")

^{:refer vinyasa.reflection/.>var :added "2.1"}
(fact "extracts a class method into a namespace."

  (.>var hash-without [clojure.lang.IPersistentMap without])

  (with-out-str (eval '(clojure.repl/doc hash-without)))
  => (str "-------------------------\n"
          "vinyasa.reflection-test/hash-without\n"
          "[[clojure.lang.IPersistentMap java.lang.Object]]\n"
          "  \n"
          "member: clojure.lang.IPersistentMap/without\n"
          "type: clojure.lang.IPersistentMap\n"
          "modifiers: instance, method, public, abstract\n")

  (eval '(hash-without {:a 1 :b 2} :a))
  => {:b 2})

^{:refer vinyasa.reflection/.>ns :added "2.1"}
(fact "extracts all class methods into its own namespace."

  (map #(.sym %)
       (.>ns test.string String :private #"serial"))
  => '[serialPersistentFields serialVersionUID])
