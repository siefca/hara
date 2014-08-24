(ns hara.protocol.watch
  (:require [hara.expression.shorthand :refer [get->]]))

(defprotocol IWatch
  (-add-watch    [obj k f opts])
  (-remove-watch [obj k opts])
  (-list-watch   [obj opts]))
