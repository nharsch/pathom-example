(ns ip-weather
  (:require
    [cheshire.core :as json]
    [com.wsscode.pathom3.connect.indexes :as pci]
    [com.wsscode.pathom3.connect.operation :as pco]
    [com.wsscode.pathom3.interface.eql :as p.eql]
    [com.wsscode.pathom3.interface.smart-map :as psm]
    [com.wsscode.edn-json :refer [edn->json json->edn]]
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

(pco/defresolver latlong->address
  [{:keys [latitude longitude]}]
  {::pco/output [:city :country :postcode :state :postcode]}
  (-> @(http/request {:url (str "https://nominatim.openstreetmap.org/reverse?format=json&lat=" latitude "&lon=" longitude)})
      :body
      (json/parse-string keyword)
      :address))

;; (pco/defresolver address->keys
;;   [{:keys [address]}]
;;   {:city (:city address)
;;    :country (:country address)})

(def env
  (pci/register [ip->lat-long
                 latlong->temperature
                 latlong->address
                 ]))

(def pathom (p.eql/boundary-interface env))

(comment
  ; here's how to pass data into boundary interface
  ; with eql
  (pathom {:pathom/entity  {:ip "192.29.213.3"}
           :pathom/eql [:state]})
  ; with AST
  )

(defn main [{:keys [ip]}]
  (println "Request temperature for the IP" ip))



(comment
  (-> {:ip "192.29.213.3"}
      ip->lat-long
      latlong->temperature)
  (->  @(http/request {:url (str "https://nominatim.openstreetmap.org/reverse?format=json&lat=" 34 "&lon=" -118)})
       :body
       (json/parse-string keyword)
       :address
       keys)
  (json/parse-string "{ \"test\" : [{ \"inner\": 1}]}" keyword)
  (p.eql/process env {:ip "192.29.213.3"} [:address])
  (p.eql/process env {:ip "192.29.213.3"} [:city])
  (p.eql/process env {:ip "192.29.213.3"} [:country])
  (p.eql/process env {:ip "192.29.213.3"} [:state])
  (p.eql/process env {:ip "192.29.213.3"} [:city :temperature])
  (p.eql/process env {:ip "192.29.213.3"} (json/parse-string "[\"city\", \"temperature\"]") #(map keyword %))
  env
  )
