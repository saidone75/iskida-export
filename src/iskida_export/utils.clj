(ns iskida-export.utils)

(require '[clojure.string :as s])

(defn creation-time [file]
  (s/trim
   (reduce
    #(str %1  " " (s/replace %2 "Z" ""))
    ""
    (s/split (.toString
              (.creationTime
               (java.nio.file.Files/readAttributes
                (.toPath file)
                java.nio.file.attribute.BasicFileAttributes
                (into-array java.nio.file.LinkOption []))))
             #"T"))))

(defn epoch-to-date [epoch]
  (.format (java.time.LocalDateTime/ofInstant (java.time.Instant/ofEpochSecond epoch) java.time.ZoneOffset/UTC) (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss")))

(defn date-to-epoch [date]
  (.toEpochSecond (.atZone (java.time.LocalDateTime/parse date (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss")) java.time.ZoneOffset/UTC)))

(defn get-items [xml]
  (->> xml
       :content
       first))

(defn get-tag-content [tag item]
  (->> item
       :content
       last
       (filter #(= (:tag %) tag))
       first
       :content
       first
       :content))

(defn get-authors-list [xml]
  (map
   (partial get-tag-content :created_by)
   (get-items xml)))

(defn get-authors [article-map]
  (into
   #{}
   (filter
    #(not (s/blank? %))
    (let [authors (:_sown_ article-map)]
      (if (not (s/blank? authors))
        (map
         #(s/replace (s/replace % #"@.*$" "") #"^accounts\." "")
         (s/split authors #",")
         )
        #{})))))

(defn get-categories [article-map]
  (let [categories (:categories article-map)]
    (if (not (s/blank? categories))
      (into #{}
            (map
             #(s/replace % #"^categories\." "")
             (s/split categories #",")
             ))
      #{})))
