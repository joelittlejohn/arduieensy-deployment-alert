(ns arduieensy-deployment-alert.daemon
  (:require [clj-http.client :as client]))

(def daemon-agent (agent ))

(defn- pipeline-state []
  (let [response (client/get "http://localhost:8000/pipeline/1338997244.json"
                             {:as :json
                              :throw-exceptions false})
        state (get-in response [:body :state])
        waiting? ((last (get-in response [:body :environmentDeployments])) :requiresInput)]
    {:state state
     :waiting? waiting?}))

(defn- badger-state [] 
  (let [response (client/get "http://localhost:8000/badger/1338997292.json"
                             {:as :json
                              :throw-exceptions false})]
    (get-in response [:body :acquireUser])))

(defn- alert-state []
  (let [pipeline-state (pipeline-state)
        badger-state (badger-state)]
    (cond (and (nil? pipeline-state) (nil? badger-state))
          [:color :none :throbbing? false]
          (nil? pipeline-state)
          [:color :green :throbbing? true}
          (some #{(pipeline-state :state)} ["INITIAL" "CREATED" "DEPLOYING" "IN_PROGRESS"])
          [:color :green :throbbing? (pipeline-state :waiting?)}
          (some #{(pipeline-state :state)} ["FAILED"])
          [:color :yellow :throbbing? (pipeline-state :waiting?)})))

(let [new-state (alert-state)
      new-color (:color new-state)
      new-throbbing? (:throbing? new-state)]
      (dosync (ref-set (orb/orb :current-color) new-color)
              (ref-set (orb/orb :throbbing?) new-throbbing?)))