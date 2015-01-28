(defproject cn.leancloud/clj-msgpack "0.4.2-SNAPSHOT"
  :url "https://github.com/leancloud/clj-msgpack"
  :license {:name "MIT"}
  :description "Messagepack serialization library for Clojure. LeanCloud fork."
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.msgpack/msgpack "0.6.11" :exclusions [junit]]]
  :deploy-repositories {"releases" :clojars})
