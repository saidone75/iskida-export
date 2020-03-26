(ns iskida-export.users
  (:gen-class))

(require '[clojure.data.csv :refer :all]
         '[clojure.data.xml :refer :all]
         '[iskida-export.config :as config]
         '[iskida-export.utils :as utils])

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
             (csv-data->maps (read-csv config/fixed-users-csv))))))

(defn gen-users []
  (spit config/users-xml-output (emit-str (xml))))
