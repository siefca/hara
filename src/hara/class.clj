(ns hara.class
  (:require [hara.class.inheritance :as hierarchy]))

(defn context-class [obj]
  (if (class? obj) obj (type obj)))

(defmacro defclassmulti [name class doc? meta? args & body])

(defmacro defclassmethod [name class doc? meta? args & body])
