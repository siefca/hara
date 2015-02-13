(ns hara.data.record)

(defn empty-record
  "creates an empty record from an existing one

  (empty-record (Database. \"localhost\" 8080))
  => (just {:host nil :port nil})"
  {:added "2.1"}
  [v]
  (.invoke ^java.lang.reflect.Method
           (.getMethod ^Class (type v) "create"
                       (doto ^"[Ljava.lang.Object;"
                         (make-array Class 1)
                         (aset 0 clojure.lang.IPersistentMap)))
           nil
           (object-array [{}])))
