(ns iskida-export.main
  (:gen-class))

(require '[iskida-export.config :as config]
         '[iskida-export.users :as users]
         '[iskida-export.pages :as pages]
         '[iskida-export.comments :as comments]
         '[immuconf.config :as immu])

(config/make-config (immu/load "resources/config_veramente.edn"))

(defn -main [& args]
  (users/gen-users)
  (def xml (pages/gen-pages))
  (comments/gen-comments xml))

(-main)
