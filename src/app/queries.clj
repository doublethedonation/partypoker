(ns app.queries
  (:require [xtdb.api :as xt]))

(defn create-account [db account-name]
  (xt/submit-tx db [[:xtdb.api/put
                     {:xt/id        (random-uuid)
                      :account/name account-name}]]))

(defn create-room [db room-name]
  (xt/submit-tx db [[:xtdb.api/put
                     {:xt/id                 (random-uuid)
                      :room/name             room-name
                      :room/current-accounts #{}}]]))

(defn create-vote [])

(defn get-all-accounts [db]
  (xt/q db '{:find  [(pull ?a [:xt/id :account/name])]
             :where [[?a :account/name]]}))

(defn get-all-rooms [db]
  (xt/q db '{:find  [(pull ?a [:xt/id :room/name :room/current-accounts])]
             :where [[?a :room/name]]}))

(defn get-room-by-name [db room-name]
  (-> (xt/q db '{:find  [(pull ?room [:xt/id :room/name :room/current-accounts])]
                 :in    [?room-name]
                 :where [[?room :room/name ?room-name]]}
            room-name)
      ffirst))

(defn enter-room [db room-name account-name]
  (let [room (get-room-by-name (xt/db db) room-name)]
    (if room
      (do (println room (update room :current-accounts conj account-name))
       (xt/submit-tx db [[:xtdb.api/put
                          (assoc room :testvar 1
                                      :current-accounts
                                      (set (conj (:current-accounts room) account-name)))]])))))



(comment
  (create-account user/!xtdb "Kanwei")
  (create-room user/!xtdb "room1")
  (create-room user/!xtdb "room3")
  (enter-room user/!xtdb "room3" "Kanwei")
  (create-room user/!xtdb "room123")
  (get-all-rooms (xt/db user/!xtdb))
  (get-room-by-name (xt/db user/!xtdb) "room2")
  (get-room-by-name (xt/db user/!xtdb) "aroomfake")
  (get-all-accounts (xt/db user/!xtdb)))


