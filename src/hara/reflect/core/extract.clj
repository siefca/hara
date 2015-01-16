(ns hara.reflect.core.extract
  (:require [clojure.string :as string]
            [hara.reflect.common :as common]
            [hara.reflect.types.element :as element]
            [hara.reflect.pretty.classes :as classes]
            [hara.reflect.pretty.args :as args]
            [hara.reflect.element multi method field constructor]
            [hara.reflect.core.query :as q]))

(defn process-if-single [args]
  (if (and (vector? (first (first args)))
           (= 1 (count (first args))))
    (first args)
    args))

(defn element-meta
  [ele]
  (-> {}
      (assoc :arglists (->> (element/element-params ele)
                            concat
                            set
                            (mapv vec)
                            (process-if-single)))
      (assoc :doc
        (if (= :multi (:tag ele)) ""
          (format "\nmember: %s\ntype: %s\nmodifiers: %s"
                  (str (.getName (:container ele))
                       "/" (:name ele))
                  (classes/class-convert (:type ele) :string)
                  (string/join ", " (map name (:modifiers ele))))))))

(defn extract-to-var
  "extracts a class method into a namespace.

  (extract-to-var 'hash-without clojure.lang.IPersistentMap 'without [])

  (with-out-str (eval '(clojure.repl/doc hash-without)))
  => (str \"-------------------------\\n\"
          \"hara.reflect.core.extract-test/hash-without\\n\"
          \"[[clojure.lang.IPersistentMap java.lang.Object]]\\n\"
          \"  \\n\"
          \"member: clojure.lang.IPersistentMap/without\\n\"
          \"type: clojure.lang.IPersistentMap\\n\"
          \"modifiers: instance, method, public, abstract\\n\")

  (eval '(hash-without {:a 1 :b 2} :a))
  => {:b 2}"
  {:added "2.1"}
  ([varsym class method selectors]
   (extract-to-var *ns* varsym class method selectors))
  ([nssym varsym class method selectors]
    (let [v  (intern nssym varsym (q/query-class class (cons (str method) (cons :# selectors))))]
      (alter-meta! v (fn [m] (merge m (element-meta @v))))
      v)))

(defn extract-to-ns
  "extracts all class methods into its own namespace.

  (map #(.sym %)
       (extract-to-ns 'test.string String [:private #\"serial\"]))
  => '[serialPersistentFields serialVersionUID]"
  {:added "2.1"}
  [nssym class selectors]
  (let [eles (q/list-class-elements class selectors)
        methods (distinct (map :name eles))]
    (clojure.core/create-ns nssym)
    (doall (for [method methods]
             (extract-to-var nssym (symbol method) class method selectors)))))
