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
                      :value (fn [x] (s/trim (str (s/replace x #"\n" ""))))}
                     (drop 1 (ffc-parser page)))))
        ffc-map
        (merge ffc-map {:abstract (str "<p>" (:description ffc-map) "</p><p>" (:abstract ffc-map) "</p>")})]
    (if (and (contains? ffc-map :authors)
             (not (empty? (-> ffc-map
                              (:authors)
                              (s/trim)))))
      (dissoc ffc-map :_sown_)
      (dissoc ffc-map :authors))))

