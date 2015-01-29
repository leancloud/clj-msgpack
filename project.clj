(defproject cn.leancloud/clj-msgpack "0.5.1-SNAPSHOT"
  :url "https://github.com/leancloud/clj-msgpack"
  :license {:name "MIT"}
  :description "Messagepack serialization library for Clojure. LeanCloud fork."
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.msgpack/msgpack-core "0.7.0-p5" :exclusions [junit]]]
  :deploy-repositories {"releases" :clojars})
