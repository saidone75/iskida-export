(ns iskida-export.parser
  (:gen-class))

(require '[clojure.string :as s]
         '[instaparse.core :as insta])

(def page-parser
  (insta/parser
   "article = (key value)*
    key = #'§[^§]+§'
    value = #'[^§]+'"
   :output-format :hiccup))

(defn article-map [page]
  (reduce
   #(assoc %1 (first %2) (last %2))
   {}
   (partition 2
              (insta/transform
               {:key (fn [x] (keyword (s/replace x #"§" "")))
                :value (fn [x] (str (s/replace x #"\n" "")))}
               (drop 1 (page-parser page))))))

