(ns arduieensy-deployment-alert.daemon
  (:require [clj-http.client :as http])
  (:require [arduieensy-deployment-alert.orb :as orb])
  (:require [clojure.string :as str])
  (:gen-class))

(def current-pipeline-url
  (str (System/getenv "PIPELINE_URL") "/ws/2.x/pipelines/current"))

(def current-badger-url
  (str (System/getenv "PIPELINE_URL") "/ws/2.x/badgers/current"))

(def users
  (str/split (get (System/getenv) "ALERT_USERS" "jlittlej") #"\s"))

(defn- pipeline-state []
  (let [response (http/get current-pipeline-url {:as :json
                                                 :throw-exceptions false})
        user (-> response :body :user)]
    (when (and (= 200 (response :status)) (or (empty? users) (some #{user} users)))
      {:failed? (or (-> response :body :inFailedState)
                    (-> response :body :environmentDeployments last :inFailedState))
       :waiting? (-> response :body :environmentDeployments last :requiresInput)})))

(defn- badger-state []
  (if-let [body ((http/get current-badger-url {:as :json
                                               :throw-exceptions false}) :body)]
    (let [user (body :acquireUser)]
      (if (or (empty? users) (some #{user} users))
        user))))

(defn- alert-state []
  (let [pipeline-state (pipeline-state)
        badger-state (badger-state)]
    (cond (and (nil? pipeline-state) (nil? badger-state))
          {:color :none :throbbing? false}
          (nil? pipeline-state)
          {:color :green :throbbing? true}
          (pipeline-state :failed?)
          {:color :yellow :throbbing? (pipeline-state :waiting?)}
          :else
          {:color :green :throbbing? (pipeline-state :waiting?)})))

(defn update [orb]
  (let [new-state (alert-state)
        new-color (:color new-state)
        new-throbbing? (:throbbing? new-state)]
      (dosync (ref-set (orb :current-color) new-color)
              (ref-set (orb :throbbing?) new-throbbing?))
      (Thread/sleep 2000)
      (recur orb)))

(defn -main []
  (let [orb (orb/start!)]
        (update orb)))
