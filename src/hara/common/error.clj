(ns hara.common.error)

;; ## Errors
;;
;; If we place too much importance on exceptions, exception handling code
;; starts littering through the control code. Most internal code
;; do not require definition of exception types as exceptions are
;; meant for the programmer to look at and handle.
;;
;; Therefore, the exception mechanism should get out of the way
;; of the code. The noisy `try .... catch...` control structure
;; can be replaced by a `suppress` statement so that errors can be
;; handled seperately within another function or ignored completely.
;;

(defmacro error
  "Throws an exception when called.

  (error \"This is an error\")
  => (throws Exception \"This is an error\")

  (error (Exception. \"This is an error\")
         \"This is a chained error\")
  => (throws Exception \"This is a chained error\")"
  {:added "2.0"}
  {:added "2.0"}
  [e & [opt? & more]]
  `(if (instance? Throwable ~e)
     (throw (Exception. (str ~opt? ~@more) ~e))
     (throw (Exception. (str ~e ~opt? ~@more)))))

(defmacro suppress
  "Suppresses any errors thrown in the body.

  (suppress (error \"Error\")) => nil

  (suppress (error \"Error\") :error) => :error

  (suppress (error \"Error\")
            (fn [e]
              (.getMessage e))) => \"Error\""
  {:added "2.0"}
  ([body]
     `(try ~body (catch Throwable ~'t)))
  ([body catch-val]
     `(try ~body (catch Throwable ~'t
                   (cond (fn? ~catch-val)
                         (~catch-val ~'t)
                         :else ~catch-val)))))
