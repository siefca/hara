(ns hara.data
  (:require [hara.namespace.import :as ns]
            [hara.data.map]
            [hara.data.nested]
            [hara.data.combine]
            [hara.data.complex]
            [hara.data.path]))

(ns/import
  hara.data.map      :all
  hara.data.nested   :all
  hara.data.combine  :all
  hara.data.complex  :all
  hara.data.path     :all)
