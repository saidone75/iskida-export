(ns iskida-export.users
  (:gen-class))

(require '[clojure.data.csv :refer :all]
         '[clojure.data.xml :refer :all]
         '[iskida-export.config :as config])

(defn csv-data->maps [csv-data]
  (map zipmap
       (->> (first csv-data) 
            (map keyword)
            repeat)
       (rest csv-data)))

(defn build-user [users user]
  (cons
   (element :user nil
            (element :id nil (:id user))
            (element :name nil (cdata (:username user)))
            (element :username nil (cdata (:username user)))
            (element :email nil (cdata (:email user)))
            (element :password nil (cdata (:password_hash user)))
            (element :block nil 0)
            (element :sendEmail nil 0)
            (element :group nil (cdata "[\"Public\",\"Registered\"]")))
   users))

(def xml
  (element :j2xml {:version "19.2.0"}
           (reduce
            build-user
            '()
            (csv-data->maps (read-csv csv)))))

(spit "/tmp/users.xml" (emit-str xml))
