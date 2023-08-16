(ns ip-weather
  (:require
    [cheshire.core :as json]
    [com.wsscode.pathom3.connect.indexes :as pci]
    [com.wsscode.pathom3.connect.operation :as pco]
    [com.wsscode.pathom3.interface.eql :as p.eql]
    [com.wsscode.edn-json :as ej]
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
       last
       :temp2m
       )})

(pco/defresolver latlong->address
  [{:keys [latitude longitude]}]
  {::pco/output [:city :country :postcode :state :postcode]}
  (-> @(http/request {:url (str "https://nominatim.openstreetmap.org/reverse?format=json&lat=" latitude "&lon=" longitude)})
      :body
      (json/parse-string keyword)
      :address))

(def env
  (pci/register [ip->lat-long
                 latlong->temperature
                 latlong->address
                 ]))

(def pathom (p.eql/boundary-interface env))

; hacky but it works
(defn keywordize-colon-keys
  "Recursively transform string keys that start with ':' into keywords."
  [item]
  (cond
    ; assume keys will be converted elsewhere
    (map? item) (into {} (map (fn [[k v]] [(keywordize-colon-keys k) (keywordize-colon-keys v)]) item))
    (vector? item) (mapv keywordize-colon-keys item)
    (string? item) (if (clojure.string/starts-with? item ":") (keyword (subs item 1)) item)
    :else item))

(defn decode-payload [json-string]
  (-> json-string
      json/parse-string
      ej/json-like->edn
      keywordize-colon-keys
      ))

(defn handle-query [json-string]
  (pathom (decode-payload json-string)))



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
