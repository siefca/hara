(ns hara.protocol.data)

(defprotocol IMap
  (to-map [x]))

(defprotocol IString
  (to-string [x]))

(defmulti from-string
  (fn [s & [meta]]
    (:tag meta)))

(extend-protocol IString
  clojure.lang.Symbol
  (to-string [x]
    [(str x) {:tag :symbol}])

  clojure.lang.Keyword
  (to-string [x]
    [(subs (str x) 1) {:tag :keyword}])
    
  Class
  (to-string [x]
    [(subs (str x) 1) {:tag :class}]))

(extend-protocol IMap
  java.util.Map
  (to-map [x]
    [(into {} x)
     {:tag   :type
      :type  (.getName (type x))}])

  clojure.lang.Keyword
  (to-string [x]
    [(subs (str x) 1) {:tag :keyword}])

  Class
  (to-string [x]
    [(subs (. x) 1) {:tag :class}]))


(into {}
      (java.util.Properties. {"a" 1 "b" 2}))
