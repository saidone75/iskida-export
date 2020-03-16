(ns iskida-export.main
  (:gen-class))

(require '[iskida-export.config :as config]
         '[iskida-export.users :as users]
         '[iskida-export.categories :as categories]
         '[iskida-export.pages :as pages]
         '[iskida-export.comments :as comments])

(defn -main [& args]
  (users/gen-users)
  (categories/gen-categories)
  (def xml (pages/gen-pages))
  (comments/gen-comments xml))

(-main)
