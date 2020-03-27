(ns iskida-export.pages
  (:gen-class))

(require '[clojure.data.xml :refer :all]
         '[clojure.string :as s]
         '[iskida-export.config :as config]
         '[iskida-export.parser :as parser]
         '[iskida-export.utils :as utils])

(def page-id (atom 1000))
(defn- get-page-id []
  (swap! page-id inc))

(def tags-to-ignore
  #{:description
    :related
    :online
    :status})

(defn- build-taglist [categories]
  (if (> (count (s/split categories #",")) 1)
    (reduce
     #(cons (element :tag nil (cdata %2)) %1)
     '()
     (map
      #(s/replace % #"^categories\." "")
      (s/split categories #",")))
    (cdata (s/replace categories #"^categories\." ""))))

(defn- tag-or-taglist [content]
  (if (> (count (s/split content #",")) 1)
    "taglist"
    "tag"))

(defn- images [content]
  (let [file (first (filter #(re-matches (re-pattern (str "^" (s/replace (first (s/split content #",")) #"^images\." "") "&.*$")) (.getName %)) config/image-files))]
    (if (not (nil? file))
      (let [filename (.getName file)]
        (cdata (str "{\"image_intro\":\"images\\/"
                    filename
                    "\",\"float_intro\":\"\",\"image_intro_alt\":\"\",\"image_intro_caption\":\"\",\"image_fulltext\":\"images\\/"
                    filename
                    "\",\"float_fulltext\":\"\",\"image_fulltext_alt\":\"\",\"image_fulltext_caption\":\"\"}")))
      nil)))

(defn- created-by [author]
  (let [author (s/replace (s/replace author #"@.*$" "") "accounts." "")]
    (if (= "michele" (s/replace author #",*" ""))
      (cdata "liver")
      (cdata author))))

(def tag-dictionary
  {
   :_sown_ {:name :created_by :f created-by}
   :abstract {:name :introtext :f cdata}
   :categories {:fname tag-or-taglist :f build-taglist}
   :content {:name :fulltext :f cdata}
   :image {:name :images :f images}
   :gallery {:name :images :f images}
   :title {:name :title :f cdata}
   })

(defn- translate-tag [tag]
  (if (contains? tag-dictionary (keyword tag))
    ((keyword tag) tag-dictionary)
    {:name tag}))

(defn- build-element [tag attrs content]
  (let [element
        (let [tag (translate-tag tag)]
          (element
           (if (:fname tag)
             ((:fname tag) content)
             (:name tag)
             )
           nil
           (if (:f tag)
             ((:f tag) content)
             content)))]
    (if (nil? (first (:content element)))
      nil
      element)))

(defn- build-content [%1 %2]
  (cons
   (element :content nil
            (element :alias nil
                     (cdata (s/replace (s/replace (.getName %2) #"\.ffc$" "") #"_" "-")))
            (element :state nil 1)
            (element :created nil (cdata (utils/creation-time %2)))
            (element :modified nil (cdata (utils/creation-time %2)))
            (element :publish_up nil (cdata (utils/creation-time %2)))
            (element :publish_down nil (cdata "0000-00-00 00:00:00"))
            (element :catid nil (cdata "notizie"))
            (element :id nil (get-page-id))
            (element :urls nil nil)
            (element :attribs nil nil)
            (element :version nil 1)
            (element :metakey nil nil)
            (element :metadesc nil nil)
            (element :hits nil 0)
            (element :metadata nil nil)
            (element :language nil (cdata "*"))
            (element :access nil 1)
            (element :fatured nil 0)
            (element :rating_sum nil 0)
            (element :rating_count nil 0)
            (element :canonical nil nil)
            (map
             #(build-element (key %) nil (val %))
             (->> %2
                  (slurp)
                  (parser/article-map)
                  ))) %1))

(defn- xml []
  (element :j2xml {:version "19.2.0"}
           (reduce
            build-content
            '()
            config/page-files)))

(defn gen-pages! []
  (reset! page-id 1000)
  (let [xml (xml)]
    (spit config/pages-xml-output (emit-str xml))
    xml))
