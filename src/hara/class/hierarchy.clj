(ns hara.class.hierarchy)

(defn inheritance-list
  ([cls] (inheritance-list cls ()))
  ([cls output]
     (if (nil? cls)
       output
       (recur (.getSuperclass cls) (cons cls output)))))

(defn base-list
  ([cls] (base-list cls []))
  ([cls output]
     (let [base (.getSuperclass cls)]
       (if-not base output
               (recur base
                      (conj output [base (-> (.getInterfaces cls) seq set)]))))))