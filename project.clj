(defproject iskida-export "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/data.csv "1.0.0"]
                 [org.clojure/data.xml "0.2.0-alpha6"]
                 [russellwhitaker/immuconf "0.3.0"]
                 [instaparse "1.4.10"]]
  
  :main ^:skip-aot iskida-export.main
  :target-path "target/%s"
  :resource-paths []
  :profiles {:uberjar {:aot :all}})
