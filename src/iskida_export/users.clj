(ns iskida-export.users
  (:gen-class))

(require '[clojure.data.csv :refer :all]
         '[clojure.data.xml :refer :all]
         '[clojure.string :as s]
         '[iskida-export.config :as config]
         '[iskida-export.parser :as parser]
         '[iskida-export.utils :as utils])

(def user-id (atom 1000))
(defn- get-user-id []
  (swap! user-id inc))

(defn- user-from-ffc [users user]
  (let [user-map (parser/ffc-map (slurp user))
        id (get-user-id)
        name (s/replace (.getName user) #"\..*" "")]
    (conj
     users
     {:id id
      :name name
      :username (if (contains? user-map :label)
                  (:label user-map)
                  (str (:name user-map) " " (:surname user-map)))

      :email (if (contains? user-map :email)
               (:email user-map)
               (str name id "@" config/domain))
      :password_hash nil
      :created_at (str (utils/date-to-epoch (utils/creation-time user))) 
      })))

(defn- users-from-ffc []
  (reduce
   user-from-ffc
   '()
   config/users-files))

(defn csv-data->maps [csv-data]
  (map zipmap
       (->> (first csv-data) 
            (map keyword)
            repeat)
       (rest csv-data)))

(defn- build-user [users user]
  (cons
   (element :user nil
            (element :id nil (:id user))
            (element :name nil (cdata (:username user)))
            (element :username nil (cdata (:username user)))
            (element :email nil (cdata (:email user)))
            (element :password nil (cdata (:password_hash user)))
            (element :registerDate nil (cdata (utils/epoch-to-date (Long/parseLong (:created_at user)))))
            (element :block nil 0)
            (element :sendEmail nil 0)
            (element :group nil (cdata "[\"Public\",\"Registered\"]")))
   users))

(defn- xml []
  (element :j2xml {:version "19.2.0"}
           (reduce
            build-user
            '()
            (concat
             (csv-data->maps (read-csv config/users-csv))
             (users-from-ffc)))))

(defn gen-users! []
  (reset! user-id 1000)
  (spit config/users-xml-output (emit-str (xml))))
