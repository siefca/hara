(ns hara.class
  (:require [hara.class.inheritance :refer [best-match]]
            [hara.common.checks :refer [hash-map?]]))

(def ^{:dynamic true} *register* {})

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
  very similar to defmulti, defmethod, and remove-method

    - defclassmethod
    - remove-classmethod

  (defclassmulti  prn-class [cls])
  (defclassmethod prn-class CharSequence [cls] \"Chars\")
  (defclassmethod prn-class Number [cls] \"Number\")
  (defclassmethod prn-class Float  [cls] \"Float\")

  (prn-class Float)  => \"Float\"
  (prn-class Long)   => \"Number\"
  (prn-class String) => \"Chars\"
  (prn-class (type {})) => (throws Exception)"
  {:added "2.1"}
  [name & [docstring? attr-map? args]]
  (let [varname (str *ns* "/" name)
        [docstring attr-map [[cls & more :as args]]]
        (standardize docstring? attr-map? args)]
    `(do (when (defmulti ~name ~docstring ~attr-map
                 (fn ~args
                   (best-match (get *register* ~varname)
                               ~cls)))
           (alter-var-root #'*register* (fn [m#] (assoc m# ~varname #{})))
           (defmethod ~name :default ~args
             (throw (Exception. (str "Not implemented for class: " ~cls))))))))

(defmacro defclassmethod [multi class args & body]
  (let [var (resolve multi)
        varname (str (.ns var) "/" (.sym var))
        _   (if-not (class? (resolve class))
              (throw (Exception. (str class " is not a valid class."))))]
    `(do (alter-var-root #'*register*
                         (fn [m#] (update-in m# [~varname] conj ~class)))
         (defmethod ~multi ~class ~args ~@body))))

(defmacro remove-classmethod [multi class]
  (let [var (resolve multi)
        varname (str (.ns var) "/" (.sym var))
        _   (if-not (class? (resolve class))
              (throw (Exception. (str class " is not a valid class."))))]
    `(do (alter-var-root #'*register*
                         (fn [m#] (update-in m# [~varname] disj ~class)))
         (remove-method ~multi ~class))))
