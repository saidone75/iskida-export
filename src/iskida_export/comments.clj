(ns iskida-export.comments
  (:gen-class))

(require '[clojure.data.csv :refer :all]
         '[clojure.data.xml :refer :all]
         '[clojure.string :as s]
         '[iskida-export.config :as config]
         '[iskida-export.parser :as parser]
         '[iskida-export.users :as users]
         '[iskida-export.utils :as utils])

(defn- epoch-to-date [epoch]
  (.format (java.time.LocalDateTime/ofInstant (java.time.Instant/ofEpochSecond epoch) java.time.ZoneOffset/UTC) (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss")))

(defn- build-alias-id-map [m item]
  (let [alias (filter #(= :alias (:tag %)) item)
        id (filter #(= :id (:tag %)) item)]
    (conj m {:alias (:content (first alias)) :id (:content (first id))})))

(defn- alias-id-map []
  (reduce
   build-alias-id-map
   '() 
   (map
    #(:content %)
    (:content (parse (java.io.FileReader. "/tmp/riusa.xml"))))))

(defn users-map []
  (users/csv-data->maps (read-csv config/csv)))

(defn- assoc-parent-id [comment-file]
  (let [[title timestamp] (drop 1 (re-find #"^news°([^°]*)°(\d+)\.ffc$" (.getName comment-file)))
        article-map (parser/article-map (slurp comment-file))]
    (let [alias-id-map-entry
          (if (not (s/blank? title))
            (filter
             #(= (s/replace title #"_" "-") (first (:alias %)))
             (alias-id-map))
            nil)]
      (if (not (empty? alias-id-map-entry))
        (merge article-map
               {:id (first (:id (first alias-id-map-entry)))}
               {:timestamp timestamp})
        nil))))

(defn- filter-users [username]
  (filter
   #(= (:username %) (s/replace username #"^[^\.]*\." ""))
   (users-map)))

(defn- assoc-user-id [comments comment]
  (let [user-id (:id (first (filter-users (:user comment))))]
    (conj comments (merge comment {:user-id user-id}))))

(def comments
  (reduce
   assoc-user-id
   '()
   (filter
    #(= "true" (:online %))
    (map
     assoc-parent-id
     config/comments-files))))

(def prefix "INSERT INTO `riusa_jcomments` (`path`, `level`, `object_id`, `object_group`,  `lang`, `userid`, `name`,  `title`, `comment`,  `date`, `published`, `checked_out_time`) VALUES (")

(defn- make-insert [statement comment]
  (str statement
       prefix
       "'0','0','"
       (:id comment)
       "','com_content','en-GB','"
       (cond
           (s/blank? (:user-id comment)) "604" ;; map to anonymous
           (= "10" (:user-id comment)) "427" ;; map to liver
           :else (:user-id comment))
       "','"
       (s/replace (:user comment) #"^[^\.]*\." "")
       "','"
       (if (not (s/blank? (:title comment)))
         (s/replace (:title comment) #"'" "''")
         nil
         )
       "','"
       (s/replace (:content comment) #"'" "''")
       "','"
       (epoch-to-date (Long/parseLong (:timestamp comment)))
       "','1','"
       (epoch-to-date (Long/parseLong (:timestamp comment)))
       "');\n"))

(def sql
  (reduce
   make-insert
   ""
   comments))

(defn gen-comments []
  (spit "/tmp/comments.sql" sql))
