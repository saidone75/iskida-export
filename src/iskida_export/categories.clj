(ns iskida-export.categories
  (:gen-class))

(require '[clojure.data.xml :refer :all]
         '[iskida-export.config :as config]
         '[iskida-export.parser :as parser]
         '[iskida-export.utils :as utils])

(def category-id (atom 0))
(defn get-category-id []
  (swap! category-id inc))

(defn build-tag [element-list category]
  (cons
   (element :tag nil
            (element :id nil (get-category-id))
            (element :path nil (cdata category))
            (element :extension nil (cdata "com_content"))
            (element :title nil (cdata category))
            (element :alias nil (cdata category))
            (element :published nil 1))
   element-list))

(def categories
  (reduce
   #(clojure.set/union %1 (utils/get-categories (parser/article-map (slurp %2))))
   #{}
   config/page-files))

(def xml
  (element :j2xml {:version "19.2.0"}
           (reduce
            build-tag
            '()
            categories)))

(spit "/tmp/tags.xml" (emit-str xml))
