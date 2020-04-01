(ns iskida-export.parser
  (:gen-class))

(require '[clojure.string :as s]
         '[instaparse.core :as insta])

(def ffc-parser
  (insta/parser
   "article = (key value)*
    key = #'§[^§]+§'
    value = #'[^§]+'"
   :output-format :hiccup))

(defn ffc-map [page]
  (let [ffc-map 
        (reduce
         #(assoc %1 (first %2) (last %2))
         {}
         (partition 2
                    (insta/transform
                     {:key (fn [x] (keyword (s/replace x #"§" "")))
                      :value (fn [x] (str (s/replace x #"\n" "")))}
                     (drop 1 (ffc-parser page)))))]
    (if (contains? ffc-map :authors)
      (dissoc ffc-map :_sown_)
      ffc-map)))
