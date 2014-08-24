(ns hara.string.path
  (:require [clojure.string :as string]
            [hara.common.string :as common])
  (:refer-clojure :exclude [split join contains val]))

(defn join
  "joins a sequence of elements into a path seperated value

  (path/join [\"a\" \"b\" \"c\"])
  => \"a/b/c\"

  (path/join '[a b c] '-)
  => 'a-b-c"
  {:added "2.1"}
  ([ks] (join ks "/"))
  ([ks sep]
     (if (empty? ks) nil
         (let [meta (common/to-meta (first ks))]
           (->> (filter identity ks)
                (map common/to-string)
                (string/join sep)
                (common/from-string meta))))))

(defn split
  "splits a sequence of elements into a path seperated value

  (path/split :hello/world)
  => [:hello :world]

  (path/split \"a/b/c/d\")
  => '[\"a\" \"b\" \"c\" \"d\"]"
  {:added "2.1"}
  ([k] (split k #"/"))
  ([k re]
     (cond (nil? k) []

           :else
           (let [meta (common/to-meta k)]
             (mapv #(common/from-string meta %)
                   (string/split (common/to-string k) re))))))

(defn contains
  "check that a path contains the subkey

  (path/contains :hello/world :hello)
  => true

  (path/contains \"a/b/c/d\" \"a/b/c\")
  => true"
  {:added "2.1"}
  [k subk]
  (or (= k subk)
      (.startsWith (common/to-string k)
                   (str (common/to-string subk) "/"))))

(defn path-vec
  "returns the path vector of the string/keyword/symbol

  (path/path-vec \"a/b/c/d\")
  => [\"a\" \"b\" \"c\"]"
  {:added "2.1"}
  [k]
  (or (butlast (split k)) []))

(defn path-vec?
  "check for the path vector of the string/keyword/symbol

  (path/path-vec? \"a/b/c/d\" [\"a\" \"b\" \"c\"])
  => true"
  {:added "2.1"}
  [k pv]
  (= pv (path-vec k)))

(defn path-ns
  "returns the path namespace of the string/keyword/symbol

  (path/path-ns \"a/b/c/d\")
  => \"a/b/c\""
  {:added "2.1"}
  [k]
  (join (path-vec k)))

(defn path-ns?
  "check for the path namespace of the string/keyword/symbol

  (path/path-ns? \"a/b/c/d\" \"a/b/c\")
  => true"
  {:added "2.1"}
  ([k] (< 0 (.indexOf (str k) "/")))
  ([k ns] (if-let [tkns (path-ns k)]
            (= 0 (.indexOf (str k)
                 (str ns "/")))
            (nil? ns))))

(defn path-root
  "returns the path root of the string/keyword/symbol

  (path/path-root \"a/b/c/d\")
  => \"a\""
  {:added "2.1"}
  [k]
  (first (path-vec k)))

(defn path-root?
  "check for the path root of the string/keyword/symbol

  (path/path-root? \"a/b/c/d\" \"a\")
  => true"
  {:added "2.1"}
  [k pk]
  (= pk (path-root k)))

(defn path-stem-vec
  "returns the path stem vector of the string/keyword/symbol

  (path/path-stem-vec \"a/b/c/d\")
  =>  [\"b\" \"c\" \"d\"]"
  {:added "2.1"}
  [k]
  (rest (split k)))

(defn path-stem-vec?
  "check for the path stem vector of the string/keyword/symbol

  (path/path-stem-vec? \"a/b/c/d\" [\"b\" \"c\" \"d\"])
  => true"
  {:added "2.1"}
  [k kv]
  (= kv (path-stem-vec k)))

(defn path-stem
  "returns the path stem of the string/keyword/symbol

  (path/path-stem \"a/b/c/d\")
  => \"b/c/d\""
  {:added "2.1"}
  [k]
  (join (path-stem-vec k)))

(defn path-stem?
  "check for the path stem of the string/keyword/symbol

  (path/path-stem? \"a/b/c/d\" \"b/c/d\")
  => true"
  {:added "2.1"}
  [k ks]
  (= ks (path-stem k)))

(defn val
  "returns the val of the string/keyword/symbol

  (path/val \"a/b/c/d\")
  => \"d\""
  {:added "2.1"}
  [k]
  (last (split k)))

(defn val?
  "check for the val of the string/keyword/symbol

  (path/val? \"a/b/c/d\" \"d\")
  => true"
  {:added "2.1"}
  [k z]
  (= z (val k)))
