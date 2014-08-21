(ns midje-doc.hara.outline)

[[:chapter {:title "Introduction"}]]

"`hara` provides a set of functions, best practises and code abstractions. It serves a `clojure.contrib` like 
purpose but attempts to uphold the higher principles of programming. 

 - synergistic design
 - maximal code reuse 
 - minimal code repetition and wastage
 - functional orthogonality
 - functional modularity
 - functional extensibility
 - self-documentated code
 
"

[[:chapter {:title "hara.common"}]]

"commonly used functions and forms"

[[:section {:title "common.primitives"}]]

[[:file {:src "test/hara/common/primitives_test.clj"}]]

[[:section {:title "common.checks"}]]

[[:file {:src "test/hara/common/checks_test.clj"}]]

[[:section {:title "common.error"}]]

[[:file {:src "test/hara/common/error_test.clj"}]]

[[:section {:title "common.hash"}]]

[[:file {:src "test/hara/common/hash_test.clj"}]]

[[:section {:title "common.state"}]]

[[:file {:src "test/hara/common/state_test.clj"}]]

[[:section {:title "common.watch"}]]

[[:file {:src "test/hara/common/watch_test.clj"}]]

;;-------

[[:chapter {:title "hara.function"}]]

[[:section {:title "function.args"}]]

[[:file {:src "test/hara/function/args_test.clj"}]]

[[:section {:title "function.dispatch"}]]

[[:file {:src "test/hara/function/dispatch_test.clj"}]]

;;-------

[[:chapter {:title "hara.namespace"}]]

[[:section {:title "namespace.import"}]]

[[:file {:src "test/hara/namespace/import_test.clj"}]]

[[:section {:title "namespace.resolve"}]]

[[:file {:src "test/hara/namespace/resolve_test.clj"}]]

[[:section {:title "namespace.eval"}]]

[[:file {:src "test/hara/namespace/eval_test.clj"}]]

;;-------

[[:chapter {:title "hara.class"}]]

[[:section {:title "class.checks"}]]

[[:file {:src "test/hara/class/checks_test.clj"}]]

[[:section {:title "class.inheritance"}]]

[[:file {:src "test/hara/class/inheritance_test.clj"}]]

;;-------

[[:chapter {:title "hara.expression"}]]

[[:section {:title "expression.form"}]]

[[:file {:src "test/hara/expression/form_test.clj"}]]

[[:section {:title "expression.shorthand"}]]

[[:file {:src "test/hara/expression/shorthand_test.clj"}]]

[[:section {:title "expression.compile"}]]

[[:file {:src "test/hara/expression/compile_test.clj"}]]

[[:section {:title "expression.load"}]]

[[:file {:src "test/hara/expression/load_test.clj"}]]

;;-------

[[:chapter {:title "hara.extend"}]]

[[:section {:title "extend.all"}]]

[[:file {:src "test/hara/extend/all_test.clj"}]]

[[:section {:title "extend.abstract"}]]

[[:file {:src "test/hara/extend/abstract_test.clj"}]]

