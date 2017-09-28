(ns bevl.core
  (:require 
   [yada.yada :as yada]
   [hiccup.page :refer [html5 include-css]]
   [aero.core :refer [read-config]]
   [clj-http.client :as client]
   [haversine.core :refer [haversine]]
   [cheshire.core :refer [parse-string generate-string]]))

; simple atom for exposing a global function so the server can close itself
(def closer (atom nil))

(def config (read-config "config.edn"))

(defn top-by [n k coll]
  (lazy-seq (reduce (fn [top x] (let [top (conj top x)]
    (if (> (count top) n) (disj top (first top)) top)))
      (sorted-set-by #(< (k %1) (k %2))) coll)))

(defn get-nearest-bikepoints [bike-points lat lon n]
  (top-by n :distance-from-center (map (fn [bike-point] (merge bike-point 
    {:distance-from-center (haversine 
      {:latitude lat :longitude lon}
      {:latitude (get bike-point "lat") :longitude (get bike-point "lon")})})) bike-points)))

(defn get-bikepoints-data [lat lon]
  (get-nearest-bikepoints (parse-string (get (client/get 
    (get config :api-endpoint)
    {:query-params 
      {:app_id (get config :app-id) 
       :app_key (get config :app-key)}}) :body)) lat lon 5))

(defn get-bikepoints-near-leyton []
  (get-bikepoints-data (get-in config [:leyton-coords :lat]) (get-in config [:leyton-coords :lon])))

(defn- landing-content [ctx]
  (html5
    [:head
      (include-css "https://unpkg.com/tachyons@4.8.0/css/tachyons.min.css")
      [:meta {:http-equiv "Content-type" :content "text/html; charset=utf-8"}]
      [:title "BEVL - Leyton Bikes Viewer App"]]
    [:body {:class "sans-serif"}
      [:div {:class "w-60 center mt6"}
        [:div {:class "f2 black-60"} "BEVL - Leyton Bikes Viewer App"]
        [:div {:class "mt3 black-60"} "Please login using the the combination :"
          [:span {:class "black-80 b ml3"} "u: juxt, p: juxt"]]
        [:a {:class "mt4 dib white pa2 br2 bg-orange pointer no-underline" :href "/protected"} "Login"]]]))

(defn- bikes-availability-content [ctx]
  (html5
    [:head
      (include-css "https://unpkg.com/tachyons@4.8.0/css/tachyons.min.css")
      [:meta {:http-equiv "Content-type" :content "text/html; charset=utf-8"}]
      [:title "BEVL - Leyton Bikes Viewer App"]]
    [:body {:class "sans-serif"}
      [:div {:class "w-60 center mt6"}
        [:div {:class "f2 black-60"} "BEVL - Leyton Bikes Viewer App"]
        [:div {:class "mv3 black-60"} "Welcome ! You are authenticated as user : juxt"
          [:a {:class "no-underline pointer dim blue ml2 pl2 bl b--black-10" :href "/"} "go to home"]]
        [:div {:class "mt4 black-60"}
          [:div {:class "f4 mb4 pb3 black-40 "} "Feed showing the nearest 5 bike points around Leyton"]
          [:div {:class "cf pv3 br2 b bg-washed-blue"}
            [:div {:class "w-20 fl-ns pl2"} "Id"]
            [:div {:class "w-40 fl-ns pl2"} "Location"]
            [:div {:class "w-20 fl-ns pl2"} "Remaining Bikes"]
            [:div {:class "w-20 fl-ns pl2"} "Total Bikes"]]
          [:div (for [bp (get-bikepoints-near-leyton)] [:div {:class "cf pv3 "}
            [:div {:class "w-20 fl-ns pl2"} (get bp "id")]
            [:div {:class "w-40 fl-ns pl2"} (get bp "commonName")]
            [:div {:class "w-20 fl-ns pl2"} (get (nth (get bp "additionalProperties") 6) "value")]
            [:div {:class "w-20 fl-ns pl2"} (get (nth (get bp "additionalProperties") 8) "value")]])]]]]))

(defn routes []
  ["/"
    [["" (yada/resource {:produces "text/html"
                         :response (fn [ctx] (landing-content ctx))})]
     ["protected" (yada/resource {:methods
                                 {:get {:produces "text/html"
                                        :response (fn [ctx] (bikes-availability-content ctx))}}
                                 :access-control
                                 {:scheme "Basic"
                                  :verify (fn [[user password]]
                                              (when (and (= user "juxt") (= password "juxt"))
                                               {:user user
                                                :roles #{:user}}))
                                   :authorization {:methods {:get :user}}}})]
     ["die" (yada/as-resource (fn []
                               (future (Thread/sleep 100) (@closer))
                               "shutting down in 100ms..."))]
     [true (yada/as-resource nil)]]])
    

(defn run-server-returning-promise []
  (let [listener (yada/listener (routes) {:port 3000})
        done-promise (promise)]
    (reset! closer (fn []
                     ((:close listener))
                     (deliver done-promise :done)))
    done-promise))

(defn -main
  [& args]
  (let [done (run-server-returning-promise)]
    (println "server running on port 3000... GET \"http://localhost:3000/die\" to kill")
    @done))

; (comment "to run in a repl, eval this:"
;          (def server-promise (run-server-returning-promise))
;          "then either wait on the promise:"
;          @server-promise
;          "or with a timeout"
;          (deref server-promise 1000 :timeout)
;          "or close it yourself"
;          (@closer))
