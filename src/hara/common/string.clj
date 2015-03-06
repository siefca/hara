(ns hara.common.string
  (:require [hara.protocol.string :refer :all]
            [clojure.string :as string]))

(defmulti from-string
  "meta information of keywords and symbols

  (from-string {:type clojure.lang.Symbol} \"hello/world\")
  => 'hello/world

  (from-string {:type clojure.lang.Keyword} \"hello/world\")
  => :hello/world"
  {:added "2.1"}
  (fn [meta string] (or (:type meta) meta)))

(defmethod from-string String
  [meta string]
  string)

(defmethod from-string Class
  [meta string]
  (Class/forName string))

(defmethod from-string clojure.lang.Keyword
  [meta string]
  (keyword string))

(defmethod from-string clojure.lang.Symbol
  [meta string]
  (symbol string))

(defn to-string
  "converts symbols and keywords to string representation

  (to-string 'hello/world)
  => \"hello/world\"

  (to-string :hello/world)
  => \"hello/world\""
  {:added "2.1"}
  ^String [x]
  (-to-string x))

(defn to-meta
  "meta information of keywords and symbols

  (to-meta 'hello/world)
  => {:type clojure.lang.Symbol}

  (to-meta :hello/world)
  => {:type clojure.lang.Keyword}"
  {:added "2.1"}
  [x]
  (-to-string-meta x))

(extend-type nil
  IString
  (-to-string [x] "")
  (-to-string-meta [x] {:type nil}))

(extend-type Object
  IString
  (-to-string [x] (str x))
  (-to-string-meta [x] {:type (type x)}))

(extend-type clojure.lang.Keyword
  IString
  (-to-string [x]
    (if (nil? x) "" (#'string/replace-first-char (str x) \: "")))
  (-to-string-meta [x] {:type clojure.lang.Keyword}))
