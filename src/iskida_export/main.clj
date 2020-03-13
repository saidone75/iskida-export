(ns iskida-export.main
  (:gen-class))

(require '[iskida-export.users :as users]
         '[iskida-export.categories :as categories]
         '[iskida-export.pages :as pages]
         '[iskida-export.comments :as comments])

(users/gen-users)
(categories/gen-categories)
(pages/gen-pages)
(comments/gen-comments)
