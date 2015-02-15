(ns hara.reflect.element.common
  (:require [hara.protocol.string :refer [IString]]
            [hara.reflect.types.modifiers :refer [int-to-modifiers]]
            [hara.reflect.pretty.classes :refer [class-convert]]
            [hara.reflect.util :as util]
            [hara.common.string :refer [to-string]]))

(extend-type Class
  IString
  (-to-string [x]
    (.getName x))
  (-to-string-meta [x] {:type Class}))

(extend-type java.lang.reflect.Member
  IString
  (-to-string [x]
    (.getName x))
  (-to-string-meta [x] {:type (type x)}))

(defprotocol IElement
  (get-modifiers [obj])
  (get-declaring-class [obj]))

(extend-protocol IElement
  Class
  (get-modifiers [obj] (.getModifiers obj))
  (get-declaring-class [obj] (.getDeclaringClass obj))
  java.lang.reflect.Member
  (get-modifiers [obj] (.getModifiers obj))
  (get-declaring-class [obj] (.getDeclaringClass obj)))

(def ^java.lang.reflect.Field  override
  (doto (.getDeclaredField java.lang.reflect.AccessibleObject "override")
    (.setAccessible true)))

(defn set-accessible [obj flag]
  (.set override obj flag))

(defn add-annotations [seed obj]
  (if-let [anns (seq (.getDeclaredAnnotations
                      ^java.lang.reflect.AnnotatedElement obj))]
    (->> anns
         (map (fn [^java.lang.annotation.Annotation ann] [(.annotationType ann)
                        (str ann)]))
         (into {})
         (assoc seed :annotations))
    seed))

(defn seed [tag obj]
  (let [int-m (get-modifiers obj)
        modifiers (conj (int-to-modifiers int-m tag) tag)
        modifiers (if (some #(contains? modifiers %) [:public :private :protected])
                    modifiers
                    (conj modifiers :plain))
        modifiers (if (or (contains? modifiers :static)
                          (= tag :constructor))
                    modifiers
                    (conj modifiers :instance))
        _ (if (not= tag :class) (set-accessible obj true))]
    (-> {:name (to-string obj)
         :tag  tag
         :hash (.hashCode ^Object obj)
         :container (get-declaring-class obj)
         :modifiers modifiers
         :static  (contains? modifiers :static)
         :delegate obj}
        (add-annotations obj))))

(defmacro throw-arg-exception [ele args & [header]]
  `(throw (Exception. (format  "%sMethod `%s` expects params to be of type %s, but was invoked with %s instead"
                               (if ~header ~header "")
                               (str (:name ~ele))
                               (str (:params ~ele))
                               (str (mapv type ~args))))))

(defn box-args [ele args]
  (let [params (:params ele)]
    (if (= (count params) (count args))
      (try (mapv (fn [ptype arg]
                  (util/box-arg ptype arg))
                params
                args)
           (catch Exception e
             (throw-arg-exception ele args)))
        (throw-arg-exception ele args (format "ARGS: %s <-> %s, " (count params) (count args))))))

(defn format-element-method [ele]
  (let [params (map #(class-convert % :string) (:params ele))]
    (format "#[%s :: (%s) -> %s]"
                      (:name ele)
                      (clojure.string/join ", " params)
                      (class-convert (:type ele) :string))))

(defn element-params-method [ele]
  (mapv #(symbol (class-convert % :string)) (:params ele)))
