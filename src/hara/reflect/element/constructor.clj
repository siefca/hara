(ns hara.reflect.element.constructor
  (:require [hara.reflect.common :refer :all]
            [hara.reflect.types.element :refer :all]
            [hara.reflect.element.common :refer :all]
            [hara.reflect.pretty.classes :refer [class-convert]]))

(defmethod invoke-element :constructor [ele & args]
  (let [bargs (box-args ele args)]
    (.newInstance (:delegate ele) (object-array bargs))))

(defmethod to-element java.lang.reflect.Constructor [obj]
  (let [body (seed :constructor obj)]
    (-> body
        (assoc :name "new")
        (assoc :static true)
        (assoc :type (.getDeclaringClass obj))
        (assoc :params (vec (seq (.getParameterTypes obj))))
        (element))))

(defmethod format-element :constructor [ele]
  (format-element-method ele))

(defmethod element-params :constructor [ele]
  (apply list (element-params-method ele)))
