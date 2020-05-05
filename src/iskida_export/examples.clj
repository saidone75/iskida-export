(ns iskida-export.examples
  (:gen-class))

(require '[iskida-export.config :as config]
         '[iskida-export.parser :as parser]
         '[iskida-export.utils :as utils])

;; set of authors
(reduce
 #(clojure.set/union %1 (utils/get-authors (parser/ffc-map (slurp %2))))
 #{}
 config/page-files)

;; articles without author
(map
 first
 (filter
  #(empty? (last %))
  (map
   #(list (.getName %) (utils/get-authors (parser/ffc-map (slurp %))))
   config/page-files)))
