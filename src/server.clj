(ns server
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.reload :refer [wrap-reload]]
            [ip-weather :refer [handle-query]]))

(defn parse-body [body]
  ;; Your parsing logic goes here
  ;; TODO: error handling
  (println "parse-body: " body)
  (handle-query body)
  )

(defn handle-get-request [request]
  (let [body (parse-body (slurp (:body request)))]
    (println "query returned" body)
    {:status 200
     :body body}))

(defroutes app-routes
  (GET "/api" request (handle-get-request request))
  (route/not-found "Not Found"))

(def app
  (-> (routes app-routes)
      (wrap-params)
      (wrap-keyword-params)
      (wrap-json-response)
      )
  )


(defn -main [_]
  (jetty/run-jetty (wrap-reload app) {:port 3000}))
