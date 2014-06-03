(ns hara.namespace
  (:require [hara.namespace.import :as im]
            [hara.namespace.eval]
            [hara.namespace.resolve])
  (:refer-clojure :exclude [import]))

(im/import
  hara.namespace.import   :all
  hara.namespace.eval     :all
  hara.namespace.resolve  :all)
