(ns ip-weather
  (:require
    [cheshire.core :as json]
    [com.wsscode.pathom3.connect.indexes :as pci]
    [com.wsscode.pathom3.connect.operation :as pco]
    [com.wsscode.pathom3.interface.eql :as p.eql]
    [org.httpkit.client :as http]))

(pco/defresolver ip->lat-long
  [{:keys [ip]}]
  {::pco/output [:latitude :longitude]}
  (-> (slurp (str "https://get.geojs.io/v1/ip/geo/" ip ".json"))
      (json/parse-string keyword)
      (select-keys [:latitude :longitude])))

(pco/defresolver latlong->temperature
  [{:keys [latitude longitude]}]
  {:temperature
   (-> @(http/request
          {:url (str "http://www.7timer.info/bin/api.pl?lon=" longitude
                     "&lat=" latitude
                     "&product=astro&output=json")})
       :body
       (json/parse-string keyword)
       :dataseries
       first
       :temp2m
       )})

(def env
  (pci/register [ip->lat-long
                 latlong->temperature]))

(defn main [{:keys [ip]}]
  (println "Request temperature for the IP" ip))

(comment
  (-> {:ip "192.29.213.3"}
      ip->lat-long
      latlong->temperature)
  (p.eql/process env {:ip "192.29.213.3"} [:latitude :longitude :temperature])
  env
  )
