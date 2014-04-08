(ns hara.text.string-like)

(defn coerce [obj t])

(comment
  (defcoercion
    [^Long x -> :string]
    ())

  (extend-coercion)
  )


(+> :eueu str)

(defmacro str+>
  ([obj] obj)
  ([obj trans & more]
     `(let [t#   (type ~obj)
            ret# (-> (coerce ~obj java.lang.String) ~trans ~@more)]
        (coerce ret# t#))))

(>source str)

(let [t (type :hello)
      ret (-> t (.toUpperCase))]
  (coerce ret t))

(meta #'clojure.string/upper-case)




str
