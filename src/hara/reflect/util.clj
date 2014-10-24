(ns hara.reflect.util
  (:import [java.lang.reflect Field]))

(defn box-arg
  "boxArg
  (box-arg Float/TYPE 2)
  => 2.0

  (box-arg Integer/TYPE 2.001)
  => 2

  (type (box-arg Short/TYPE 1.0))
  => java.lang.Short"
  {:added "2.1"}
  [^Class param-type ^Object arg]
  (cond (not (.isPrimitive param-type))
        (.cast param-type arg)

        (= param-type Boolean/TYPE)
        (.cast Boolean arg)

        (= param-type Character/TYPE)
        (.cast Character arg)

        (instance? Number arg)
        (condp = param-type
          Integer/TYPE (.intValue arg)
          Float/TYPE   (.floatValue arg)
          Double/TYPE  (.doubleValue arg)
          Long/TYPE    (.longValue arg)
          Short/TYPE   (.shortValue arg)
          Byte/TYPE    (.byteValue arg))

        :else
        (throw (ClassCastException.
                (format "Unexpected param type, expected: %s, given: %s"
                        param-type (-> arg .getClass .getName))))))

(defn set-field [^Field field ^Object obj ^Object val]
  (let [ftype (.getType field)]
    (cond (-> ftype .isPrimitive not)
          (.set field obj (box-arg ftype val))

          (= ftype Boolean)
          (.setBoolean obj (.cast Boolean val))

          (= ftype Character)
          (.setChar obj (.cast Character val))

          (instance? Number val)
          (condp = ftype
            Integer (.setInt obj (.intValue val))
            Float   (.setFloat obj (.floatValue val))
            Double  (.setDouble obj (.doubleValue val))
            Long    (.setLong obj (.longValue val))
            Short   (.setShort obj (.shortValue val))
            Byte    (.setByte obj (.byteValue val)))

          :else
          (throw (ClassCastException.
                  (format "Unexpected param type, expected: %s, given: %s"
                        ftype (-> val .getClass .getName)))))))

(defn param-arg-match
  "(param-arg-match Double/TYPE Float/TYPE)
 => true

 (param-arg-match Float/TYPE Double/TYPE)
 => true

 (param-arg-match Integer/TYPE Float/TYPE)
 => false

 (param-arg-match Byte/TYPE Long/TYPE)
 => false

 (param-arg-match Long/TYPE Byte/TYPE)
 => true

 (param-arg-match Long/TYPE Long)
 => true

 (param-arg-match Long Byte)
 => false

 (param-arg-match clojure.lang.PersistentHashMap java.util.Map)
 => false

 (param-arg-match java.util.Map clojure.lang.PersistentHashMap)
 => true"
  {:added "2.1"}
  [^Class param-type ^Class arg-type]
  (cond (nil? arg-type)
        (-> param-type .isPrimitive not)

        (or (= param-type arg-type)
            (-> param-type (.isAssignableFrom arg-type)))
        true

        :else
        (condp = param-type
          Integer/TYPE (or (= arg-type Integer)
                           (= arg-type Long)
                           (= arg-type Long/TYPE)
                           (= arg-type Short/TYPE)
                           (= arg-type Byte/TYPE))
          Float/TYPE   (or (= arg-type Float)
                           (= arg-type Double/TYPE))
          Double/TYPE  (or (= arg-type Double)
                           (= arg-type Float/TYPE))
          Long/TYPE    (or (= arg-type Long)
                           (= arg-type Integer/TYPE)
                           (= arg-type Short/TYPE)
                           (= arg-type Byte/TYPE))
          Character/TYPE (= arg-type Character)
          Short/TYPE     (= arg-type Short)
          Byte/TYPE      (= arg-type Byte)
          Boolean        (= arg-type Boolean)
          false)))

(defn is-congruent [params args]
  (cond (nil? args)
        (= 0 (count params))

        (= (count args) (count params))
        (-> (map param-arg-match params args)
            (every? #(= true %)))

        :else false))
