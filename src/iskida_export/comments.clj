(ns iskida-export.comments
  (:gen-class))

(require '[clojure.data.csv :refer :all]
         '[clojure.data.xml :refer :all]
         '[clojure.string :as s]
         '[iskida-export.config :as config]
         '[iskida-export.parser :as parser]
         '[iskida-export.users :as users]
         '[iskida-export.utils :as utils]
         '[iskida-export.pages :as pages])

(defn- build-alias-id-map [m item]
  (let [alias (filter #(= :alias (:tag %)) item)
        id (filter #(= :id (:tag %)) item)]
    (conj m {:alias (:content (first (:content (first alias))))
             :id (apply str (:content (first id)))})))

(defn- alias-id-map [xml]
  (reduce
   build-alias-id-map
   '() 
   (map
    #(:content %)
    (first (:content xml)))))

(def alias-id-map-memo
  (memoize alias-id-map))

(defn users-map []
  (users/csv-data->maps (read-csv config/users-csv)))

(defn- assoc-parent-id [xml comment-file]
  (let [[title t1 t2] (drop 1 (re-find #"^news°([^°]*)°(\d+)°?(\d+)?\.ffc$" (.getName comment-file)))
        article-map (parser/article-map (slurp comment-file))]
    (let [alias-id-map-entry
          (if (not (s/blank? title))
            (filter
             #(= (s/replace title #"_" "-") (:alias %))
             (alias-id-map-memo xml))
            nil)]
      (if (not (empty? alias-id-map-entry))
        (merge article-map
               {:id (:id (first alias-id-map-entry))}
               {:timestamp (if (nil? t2) t1 t2)})
        nil))))

(defn- filter-users [username]
  (filter
   #(= (:username %) (s/replace username #"^[^\.]*\." ""))
   (users-map)))

(def filter-users-memo
  (memoize filter-users))

(defn- assoc-user-id [comments comment]
  (let [user-id (:id (first (filter-users-memo (:user comment))))]
    (conj comments (merge comment {:user-id user-id}))))

(defn- comments [xml]
  (reduce
   assoc-user-id
   '()
   (filter
    #(= "true" (:online %))
    (map
     #(assoc-parent-id xml %)
     config/comment-files))))

(defn- alter-table [table-prefix]
  (str
   "SET sql_mode='';\n"
   "ALTER TABLE "
   table-prefix
   "jcomments DEFAULT CHARACTER SET utf8mb4, MODIFY comment TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL;\n"))

(def set-names "SET NAMES utf8mb4;\n")

(defn- prefix []
  (str
   "INSERT INTO `"
   config/table-prefix
   "jcomments` (`path`, `level`, `object_id`, `object_group`,  `lang`, `userid`, `name`,  `title`, `comment`,  `date`, `published`, `checked_out_time`) VALUES ("))

(defn- make-insert [statement comment]
  (str statement
       (prefix)
       "'0','0','"
       (:id comment)
       "','com_content','it-IT','"
       (cond
         (s/blank? (:user-id comment)) "9" ;; map to anonymous
         (= "10" (:user-id comment)) "2" ;; map to liver
         :else (:user-id comment))
       "','"
       (s/replace (s/replace (:user comment) #"^[^\.]*\." "") #"'" "''")
       "','"
       (if (not (s/blank? (:title comment)))
         (s/replace (:title comment) #"'" "''")
         nil
         )
       "','"
       (s/replace (:content comment) #"'" "''")
       "','"
       (utils/epoch-to-date (Long/parseLong (:timestamp comment)))
       "','1','"
       (utils/epoch-to-date (Long/parseLong (:timestamp comment)))
       "');\n"))

(defn- sql [xml]
  (reduce
   make-insert
   (str (alter-table config/table-prefix) set-names)
   (comments xml)))

(defn gen-comments [xml]
  (spit config/comments-sql-output (sql xml)))
