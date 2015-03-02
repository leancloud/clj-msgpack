(defproject cn.leancloud/clj-msgpack "0.6.0"
  :url "https://github.com/leancloud/clj-msgpack"
  :license {:name "MIT"}
  :description "Messagepack serialization library for Clojure. LeanCloud fork."
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.msgpack/msgpack-core "0.7.0-p7" :exclusions [junit]]]
  :deploy-repositories {"releases" :clojars}
  :jvm-opts ["-Xmx256m"])
