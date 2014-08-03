(ns hara.function
  (:require [hara.namespace.import :as ns]
            [hara.function.args]
            [hara.function.dispatch]))

(ns/import
 hara.function.args      :all
 hara.function.dispatch  :all)
