(ns iskida-export.config
  (:gen-class))

;; list of page files
(def page-files
  (filter
   #(re-matches #".*\.ffc" (.getName %))
   (file-seq (clojure.java.io/file "/home/saidone/workspace-clojure/iskida-export/resources/users_riusa/news"))))

;; list of image files
(def image-files
  (filter
   #(re-matches #".*\.(jpg|jpeg|gif)" (.getName %))
   (file-seq (clojure.java.io/file "/home/saidone/workspace-clojure/iskida-export/resources/users_riusa/images"))))

;; comments
(def comments-files
  (filter
   #(re-matches #"^news.*\.ffc" (.getName %))
   (file-seq (clojure.java.io/file "/home/saidone/workspace-clojure/iskida-export/resources/users_riusa/comments"))))

;; users CSV
(def csv (slurp "/home/saidone/workspace-clojure/iskida-export/resources/users_riusa.csv"))
