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

(defn- email-valid? [email]
  (and (> 50 (count email))
       (re-matches
        #".+\@.+\..+"
        email)))

(defn- user-from-ffc [users user]
  (let [user-map (parser/ffc-map (slurp user))
        id (get-user-id)
        name (s/replace (.getName user) #"\..*" "")]
    (conj
     users
     {:id id
      :name (if (contains? user-map :label)
              (:label user-map)
              (str (:name user-map) " " (:surname user-map)))
      :username name 

      :email (if (and (contains? user-map :email) (not (s/blank? (:email user-map))))
               (:email user-map)
               (str (subs name 0 (min 8 (count name))) "_" id "@" config/domain))
      :password_hash nil
      :created_at (str (utils/get-epoch-from-file user))
      :is_fake (re-matches #".*fakeaccounts.*" (.getPath user))
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
            (element :name nil (cdata (s/replace (:username user) #"'" "`")))
            (element :username nil (cdata (s/replace (:username user) #"'" "`")))
            (element :email nil (cdata
                                 (if (email-valid? (:email user))
                                   (:email user)
                                   (do
                                     (print "invalid email for user \"" (:name user) "\" --> " (:email user) "\n")
                                     (str "invalid_" (:id user) "@" config/domain)))))
            (element :password nil (cdata (:password_hash user)))
            (element :registerDate nil (cdata (utils/epoch-to-date (Long/parseLong (:created_at user)))))
            (element :block nil (if (:is_fake user)
                                  1
                                  0))
            (element :sendEmail nil 0)
            (element :group nil (cdata "[\"Public\",\"Registered\"]")))
   users))

(defn- already-present? [user filtered-users]
  [(not (empty? (filter #(= (:username user) (:username %)) filtered-users)))
   (not (empty? (filter #(= (:email user) (:email %)) filtered-users)))])

(defn- duplicate-user [user [username email]]
  (merge user
         (if username
           {:username (str "dup_username_" (:id user) "_" (:username user))}
           nil)
         (if email
           {:email (str "dup_email_" (:id user) "_" (:email user))}
           nil)))

(defn- build-user-list []
  (loop [user-list
         (concat
          (users-from-ffc)
          (csv-data->maps (read-csv config/users-csv)))
         filtered-users '()]
    (if (empty? user-list)
      filtered-users
      (let [current-user (first user-list)]
        (recur
         (rest user-list)
         (let [p (already-present? current-user filtered-users)]
           (if (not (= [false false] p))
             (conj filtered-users (duplicate-user current-user p))
             (conj filtered-users current-user))
           )
         )))))

(defn- xml []
  (element :j2xml {:version "19.2.0"}
           (reduce
            build-user
            '()
            (build-user-list))))

(defn gen-users! []
  (reset! user-id 1000)
  (spit config/users-xml-output (emit-str (xml))))
