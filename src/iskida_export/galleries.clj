(ns iskida-export.galleries
  (:gen-class)
  (:import (org.jsoup Jsoup)
           (org.jsoup.safety Whitelist)))

(require '[clojure.data.xml :refer :all]
         '[clojure.string :as s]
         '[iskida-export.config :as config]
         '[iskida-export.parser :as parser]
         '[iskida-export.users :as users]
         '[iskida-export.utils :as utils])

(def gallery-id (atom 1))
(defn- get-gallery-id []
  (swap! gallery-id inc))

(def image-id (atom 1))
(defn- get-image-id []
  (swap! image-id inc))

(def lft-id (atom 1))
(defn- get-lft-id []
  (swap! lft-id inc))

(defn- get-rgt-id []
  (swap! lft-id inc))

(def asset-id (atom 9000))
(defn- get-asset-id []
  (swap! asset-id inc))

(def image-ordering (atom 0))
(defn- get-image-ordering []
  (swap! image-ordering inc))

(defn filter-owner [owner]
  (if owner
    (-> owner
        (s/trim)
        (s/replace #"," "")
        (s/replace #"^(partners|fakeaccounts|accounts|staff)\." "")
        (s/replace #"@.*$" ""))
    ""))

(defn parse-gallery [galleries gallery-file]
  (let [gallery-map (parser/ffc-map  (slurp gallery-file))]
    (assoc
     galleries
     (get-gallery-id)
     {:title (:title gallery-map)
      :content (map #(s/replace % #"^[ ]*images\." "") (s/split (:gallery gallery-map) #","))})))

(defn parse-image-ffc [image-name]
  (parser/ffc-map
   (slurp (str "resources/users_veramente/images/" image-name ".ffc"))))

(defn create-image [path gallery-id image-name]
  (println (str path " " gallery-id " " image-name))
  (let [image-map (parse-image-ffc image-name)]
    (spit config/galleries-sh (str "#'"image-name "&main.jpg'\n") :append true)
    (spit config/galleries-sh (str "cp -v '" image-name "&main.jpg' " "joomgallery/details/" path "/" image-name ".jpg" "\n") :append true)
    (spit config/galleries-sh (str "cp -v '" image-name "&main.jpg' " "joomgallery/originals/" path "/" image-name ".jpg" "\n") :append true)
    (spit config/galleries-sh (str "mogrify -verbose -path joomgallery/thumbnails/" path " -thumbnail 160x160 joomgallery/details/" path "/" image-name ".jpg" "\n") :append true)
    (str "INSERT INTO `"
         config/table-prefix
         "joomgallery` (`id`, `asset_id`, `catid`, `imgtitle`, `alias`, `imgauthor`, `imgtext`, `imgdate`, `imgfilename`, `imgthumbname`, `published`, `approved`, `ordering`, `access`, `params`, `metakey`, `metadesc`) VALUES ("
         (get-image-id) ","
         (get-asset-id) ","
         gallery-id ","
         "'" (s/replace (:title image-map) #"'" "''") "'" ","
         "'" (s/replace (s/replace (.toLowerCase (:title image-map)) #" " "-") #"'" "-") "'" ","
         "'" (filter-owner (:_sown_ image-map)) "'" ","
         ;;         "'" (:description image-map) "'" ","
         "'" (Jsoup/clean (s/replace (s/replace (:description image-map) #"'" "''") #"\\" "") (Whitelist.)) "'" ","
         "'" (utils/creation-time (clojure.java.io/file (str "resources/users_veramente/images/" image-name ".ffc"))) "'" ","
         "'" (str image-name ".jpg") "'" ","
         "'" (str image-name ".jpg") "'" ","
         "1" ","
         "1" ","
         (get-image-ordering) ","
         "5" ","
         "\"\"" ","
         "\"\"" ","         
         "\"\""
         ");\n")))

(defn create-gallery [sql gallery]
  (let [sql 
        (str sql
             "INSERT INTO `"
             config/table-prefix
             "joomgallery_catg` (`cid`, `name`, `alias`, `parent_id`, `lft`, `rgt`, `asset_id`, `level`, `published`, `catpath`, `access`, `params`, `metakey`, `metadesc`, `exclude_toplists`, `exclude_search`) VALUES ("
             (key gallery) ","
             "'" (s/replace (.toLowerCase (:title (val gallery))) #" " "_") "'" ","
             "'" (:title (val gallery)) "'" ","
             "1" ","
             (get-lft-id) ","
             (get-rgt-id) ","
             (get-asset-id) ","
             "1" ","
             "1" ","
             "'" (s/replace (.toLowerCase (:title (val gallery))) #" " "_") "'" ","
             "5" ","
             "\"\"" ","
             "\"\"" ","
             "\"\"" ","
             "0" ","
             "0" ");\n"
             )]
    (spit config/galleries-sh (str "mkdir -v -p joomgallery/details/" (s/replace (.toLowerCase (:title (val gallery))) #" " "_") "\n") :append true)
    (spit config/galleries-sh (str "mkdir -v -p joomgallery/originals/" (s/replace (.toLowerCase (:title (val gallery))) #" " "_") "\n") :append true)
    (spit config/galleries-sh (str "mkdir -v -p joomgallery/thumbnails/" (s/replace (.toLowerCase (:title (val gallery))) #" " "_") "\n") :append true)
    (str sql
         (apply str
                (do
                  (reset! image-ordering 04)
                  (map
                   (partial create-image (s/replace (.toLowerCase (:title (val gallery))) #" " "_") (key gallery))
                   (:content (val gallery))))))))

(spit config/galleries-sh "")

(spit config/galleries-sql-output
      (do (reset! gallery-id 1)
          (reset! image-id 1)
          (reset! asset-id 9000)
          (let [galleries
                (reduce
                 parse-gallery
                 {}
                 config/galleries)]
            (reduce
             create-gallery
             ""
             galleries))))
