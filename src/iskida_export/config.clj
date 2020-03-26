(ns iskida-export.config
  (:require [immuconf.config :as immu])
  (:gen-class))

;; list of page files
(def page-files)

;; list of image files
(def image-files)

;; list of comment files
(def comment-files)

;; users CSV
(def users-csv)

;; fixed users CSV
(def fixed-users-csv)

;; XML output file for pages
(def pages-xml-output)

;; joomla table prefix
(def table-prefix)

;; SQL output file for comments
(def comments-sql-output)

;; XML output file for users
(def users-xml-output)

(defn make-config [config]
  (alter-var-root
   #'page-files
   (constantly
    (filter
     #(re-matches #".*\.ffc" (.getName %))
     (file-seq (clojure.java.io/file (immu/get config :pages-dir))))))
  (alter-var-root
   #'image-files
   (constantly
    (filter
     #(re-matches #".*\.(jpg|jpeg|gif|png)" (.getName %))
     (file-seq (clojure.java.io/file (immu/get config :images-dir))))))
  (alter-var-root
   #'comment-files
   (constantly
    (filter
     #(re-matches #"^news.*\.ffc" (.getName %))
     (file-seq (clojure.java.io/file (immu/get config :comments-dir))))))
  (alter-var-root
   #'users-csv
   (constantly
    (slurp (immu/get config :users-csv))))
  (alter-var-root
   #'fixed-users-csv
   (constantly
    (slurp (immu/get config :fixed-users-csv))))
  (alter-var-root
   #'pages-xml-output
   (constantly
    (immu/get config :pages-xml-output)))
  (alter-var-root
   #'table-prefix
   (constantly
    (immu/get config :table-prefix)))
  (alter-var-root
   #'comments-sql-output
   (constantly
    (immu/get config :comments-sql-output)))
  (alter-var-root
   #'users-xml-output
   (constantly
    (immu/get config :users-xml-output)))
  nil)
