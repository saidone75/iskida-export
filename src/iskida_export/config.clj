(ns iskida-export.config
  (:require [immuconf.config :as immu])
  (:gen-class))

(def config
  (immu/load "resources/config_riusa.edn"))

;; list of page files
(def page-files
  (filter
   #(re-matches #".*\.ffc" (.getName %))
   (file-seq (clojure.java.io/file (immu/get config :page-dir)))))

;; list of image files
(def image-files
  (filter
   #(re-matches #".*\.(jpg|jpeg|gif|png)" (.getName %))
   (file-seq (clojure.java.io/file (immu/get config :image-dir)))))

;; list of comment files
(def comments-files
  (filter
   #(re-matches #"^news.*\.ffc" (.getName %))
   (file-seq (clojure.java.io/file (immu/get config :comment-dir)))))

;; users CSV
(def users-csv
  (slurp (immu/get config :users-csv)))

;; fixed users CSV
(def fixed-users-csv
  (slurp (immu/get config :fixed-users-csv)))
