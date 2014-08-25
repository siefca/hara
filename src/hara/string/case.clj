(ns hara.string.case
  (:require [clojure.string :as string]
            [hara.common.string :as common]))

(defn re-sub
  [value pattern sub-func]
  (loop [matcher (re-matcher pattern value)
         result []
         last-end 0]
    (if (.find matcher)
      (recur matcher
             (conj result
                   (.substring value last-end (.start matcher))
                   (sub-func (re-groups matcher)))
             (.end matcher))
      (apply str (conj result (.substring value last-end))))))

(def hump-pattern #"[a-z0-9][A-Z]")
(def non-camel-separator-pattern #"[_| |\-][A-Za-z]")
(def non-snake-separator-pattern #"[ |\-]")
(def non-spear-separator-pattern #"[ |\_]")

(defn separate-camel-humps [value]
  (re-sub value hump-pattern #(string/join " " (seq %))))

(defn title-case
  "converts a string-like object to a title case string

  (title-case \"helloWorld\")

  => \"Hello World\"

  (title-case :hello-world)
  => \"Hello World\""
  {:added "2.1"}
  [value]
  (->> (-> (common/to-string value)
           (separate-camel-humps)
           (string/split #"[ |\-|_]"))
       (map string/capitalize)
       (string/join " ")))

(defn lower-case
  "converts a string-like object to a lower case string

  (lower-case \"helloWorld\")
  => \"hello world\"

  (lower-case 'hello-world)
  => \"hello world\""
  {:added "2.1"}
  [value]
  (->> (-> (common/to-string value)
           (separate-camel-humps)
           (string/split #"[ |\-|_]"))
       (map string/lower-case)
       (string/join " ")))

(defn camel-case
  "converts a string-like object to camel case representation

  (camel-case :hello-world)
  => :helloWorld

  (camel-case 'hello_world)
  => 'helloWorld"
  {:added "2.1"}
  [value]
  (let [meta (common/to-meta value)
        value (common/to-string value)]
    (->> #(string/upper-case (apply str (rest %)))
         (re-sub value non-camel-separator-pattern)
         (common/from-string meta ))))

(defn snake-case
  "converts a string-like object to snake case representation

  (snake-case :hello-world)
  => :hello_world

  (snake-case 'helloWorld)
  => 'hello_world"
  {:added "2.1"}
  [value]
  (let [meta (common/to-meta value)
        value (common/to-string value)]
    (-> (separate-camel-humps value)
        (string/lower-case)
        (string/replace non-snake-separator-pattern "_")
        (->> (common/from-string meta)))))

(defn spear-case
  "converts a string-like object to spear case representation

  (spear-case :hello_world)
  => :hello-world

  (spear-case 'helloWorld)
  => 'hello-world"
  {:added "2.1"}
  [value]
  (let [meta (common/to-meta value)
        value (common/to-string value)]
    (-> (separate-camel-humps value)
        (string/lower-case)
        (string/replace non-spear-separator-pattern "-")
        (->> (common/from-string meta)))))
