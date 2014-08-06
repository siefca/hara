(ns hara.class.multi
  (:require [hara.class.inheritance :refer [best-match]]
            [hara.common.checks :refer [hash-map?]]))

(def ^:dynamic *register* {})

(defn- standardize [& [docstring? attr-map? & rest]]
  (cond (and (string? docstring?)
             (hash-map? attr-map?))
        [docstring? attr-map? rest]

        (and (string? docstring?)
             (not (hash-map? attr-map?)))
        [docstring? {} (cons attr-map? rest)]

        (hash-map? docstring?)
        ["" docstring? (cons attr-map? rest)]

        :else ["" {} (->> rest (cons attr-map?) (cons docstring?)) ]))

(defmacro defclassmulti
  "Defines class-based multimethod dispatch. Supporting methods are
  very similar to the built-in defmulti, although this will dispatch on
  the value of the class. Used mainly for metaprogramming.

  (defclassmulti  display [cls] (type cls))
  (defclassmethod display CharSequence [cls] \"Chars\")
  (defclassmethod display Number [cls] \"Number\")
  (defclassmethod display Double  [cls] \"Double\")

  (display 1.0)  => \"Double\"
  (display (long 1)) => \"Number\"
  (display \"hello\") => \"Chars\"
  (display {}) => (throws Exception)
  "
  {:added "2.1"}
  [name & more]
  (let [varname (str *ns* "/" name)
        [docstring attr-map [[cls & nargs :as args] & body]]
        (apply standardize more)]
    `(when-not (get *register* ~varname)
       (let [rec# {:multi (clojure.lang.MultiFn. ~varname
                                                (fn [~'&type ~cls ~@nargs] ~'&type)
                                                :default #'clojure.core/global-hierarchy)
                  :types #{}}]
         (alter-var-root #'*register*
                         (fn [reg#] (assoc reg# ~varname rec#))))

       (defn ~name
         ~docstring
         ~(assoc attr-map :multi varname)
         ~args
         (if-let [rec# (get *register* ~varname)]
           (let [{:keys ~'[multi types]} rec#
                 cls# ~(if (or (empty? body)
                               (-> body first nil?))
                         cls
                         `(do ~@body))
                 ~'_  (if-not (class? cls#) (throw (Exception. (str "input " cls# " is not of type class."))))
                 ~'&type (best-match ~'types cls#)]
             (if ~'&type
               (~'multi ~'&type ~@args)
               (throw (Exception. (format "classmulti: %s not implemented for %s"
                                          ~varname
                                          cls#))))))))))

(defmacro remove-classmulti
  "Uninstalls the classmulti method.

  (defclassmulti  example [cls])
  example => fn?

  (remove-classmulti example)
  example => #(instance? clojure.lang.Var$Unbound %)"
  {:added "2.1"}
  [multi]
  (let [varname (or (-> multi resolve meta :multi)
                    (str *ns* "/" multi))]
    `(when (get *register* ~varname)
       (alter-var-root #'*register*
                       (fn [reg#] (dissoc reg# ~varname)))
       (if-let [v# (-> ~varname symbol resolve)]
         (ns-unmap (.ns v#) (.sym v#)))
       true)))

(defmacro defclassmethod
  "Defines a class specific multimethod.

  (defclassmulti  display [cls m])
  (defclassmethod display Object [cls m] m)
  (display String {}) => {}

  (defclassmethod display String [cls m] (str m))
  (display String {}) => \"{}\"
  "
  {:added "2.1"}
  [multi dispatch-cls args & body]
  (let [varname (or (-> multi resolve meta :multi)
                    (str *ns* "/" multi))
        _   (if-not (class? (resolve dispatch-cls))
              (throw (Exception. (str dispatch-cls " is not a valid class."))))]
    `(when (get *register* ~varname)
       (alter-var-root #'*register*
                       (fn [reg#]
                         (-> reg# 
                             (get-in [~varname :multi])
                             (.addMethod ~dispatch-cls
                                         (fn [~'&type ~@args]
                                           ~@body)))
                         (-> reg#
                             (update-in [~varname :types] conj ~dispatch-cls))))
       ~multi)))

(defmacro remove-classmethod
  "Removes a class specific multimethod.
  (defclassmulti  display [cls])
  (defclassmethod display Object [cls] \"Object\")
  (display String) => \"Object\"

  (remove-classmethod display Object)
  (display String) => (throws Exception)
  "
  {:added "2.1"}
  [multi dispatch-cls]
  (let [varname (or (-> multi resolve meta :multi)
                    (str *ns* "/" multi))
        _   (if-not (class? (resolve dispatch-cls))
              (throw (Exception. (str dispatch-cls " is not a valid class."))))]
    `(when (get *register* ~varname)
       (alter-var-root #'*register*
                       (fn [reg#]
                         (-> reg# 
                             (get-in [~varname :multi])
                             (.removeMethod ~dispatch-cls))
                         (-> reg#
                             (update-in [~varname :types] disj ~dispatch-cls))))
       ~multi)))

(defmacro remove-all-classmethods
  "Removes all class specific multimethods.
  (defclassmulti  display [cls])
  (defclassmethod display Object [cls] \"Object\")
  (display String) => \"Object\"

  (remove-all-classmethods display)
  (display String) => (throws Exception)
  "
  {:added "2.1"}
  [multi]
  (let [varname (or (-> multi resolve meta :multi)
                    (str *ns* "/" multi))]
    `(when (get *register* ~varname)
       (alter-var-root #'*register*
                       (fn [reg#]
                         (-> reg# 
                             (get-in [~varname :multi])
                             (.reset))
                         (-> reg#
                             (update-in [~varname :types] empty))))
       ~multi)))

(defmacro list-all-classmethods
  "Lists all class specific multimethods.
  (defclassmulti display [x])
  (defclassmethod display Object [x])
  (defclassmethod display String [x])
  
  (-> (list-all-classmethods display) keys)
  => (just [Object String] :in-any-order)
  "
  {:added "2.1"}
  [multi]
  (let [varname (or (-> multi resolve meta :multi)
                    (str *ns* "/" multi))]
    `(if-let [entry# (get *register* ~varname)]
      (->> (:types entry#)
           (map #(vector % (.getMethod (:multi entry#) %)))
           (into {})))))  
