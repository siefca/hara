(ns hara.reflect
  (:require [hara.reflect.core apply delegate hierarchy import query-class query-instance]
            [hara.namespace.import :as ns])
  (:refer-clojure :exclude [.> .* .? .% .%> >ns >var]))

(ns/import
 hara.reflect.core.apply          [.>]
 hara.reflect.core.delegate       [delegate]
 hara.reflect.core.hierarchy      [.% .%>]
 hara.reflect.core.import         [>ns >var]
 hara.reflect.core.query-class    [.?]
 hara.reflect.core.query-instance [.*])
