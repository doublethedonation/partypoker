(ns app.room-list
  (:require #?(:clj [app.xtdb-contrib :as db])
            [app.queries :as queries]
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [hyperfiddle.electric-ui4 :as ui]
            [xtdb.api #?(:clj :as :cljs :as-alias) xt]))

(e/def !xtdb)
(e/def db) ; injected database ref; Electric defs are always dynamic

(e/defn RoomItem [id]
  (e/server
    (let [e (xt/entity db id)]
      (e/client
        (dom/div
         (dom/label (dom/props {:for id}) (dom/text (e/server (:room/name e)))))))))

(e/defn RoomSubmit [F]
  (dom/input
   (dom/props {:placeholder "Room name"})
   (dom/on "keydown" (e/fn [e]
                       (when (= "Enter" (.-key e))
                         (when-some [v (contrib.str/empty->nil (-> e .-target .-value))]
                           (new F v)    ;'new' is used to call electric objects/functions?
                           (set! (.-value dom/node) "")))))))

(e/defn RoomCreate []
  (e/client
   (RoomSubmit. (e/fn [v]
                  (e/server
                   (queries/create-room !xtdb v))))))

#?(:clj
   (defn room-records [db]
     (->> (queries/get-all-rooms (xt/db user/!xtdb))
          (map first)
          (sort-by :room/name)
          vec)))

(e/defn Room-list []
  (e/server
   (binding [!xtdb user/!xtdb
             db (new (db/latest-db> user/!xtdb))]
     (e/client
      (dom/link (dom/props {:rel :stylesheet :href "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css"}))
      (dom/h1 (dom/text "Planning Poker Rooms"))
      (dom/div
       (dom/text "Enter room name:")
       (RoomCreate.)
       (dom/div
        (dom/style {:padding-top "10px"})
        (dom/p
         (dom/strong 
          (dom/text "Rooms:")))
        (e/server
         (e/for-by :xt/id [{:keys [xt/id]} (e/offload #(room-records db))]
                   (RoomItem. id)))))))))
