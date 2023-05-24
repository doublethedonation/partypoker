(ns app.user-list
  (:require #?(:clj [app.xtdb-contrib :as db])
            [app.queries :as queries]
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [hyperfiddle.electric-ui4 :as ui]
            [xtdb.api #?(:clj :as :cljs :as-alias) xt]))

(e/def !xtdb)
(e/def db) ; injected database ref; Electric defs are always dynamic

(e/defn UserItem [id]
  (e/server
    (let [e (xt/entity db id)]
      (e/client
        (dom/div
         (dom/label (dom/props {:for id}) (dom/text (e/server (:account/name e)))))))))

(e/defn UserSubmit [F]
  ;; Custom input control using lower dom interface for Enter handling
  (dom/input
   (dom/props {:placeholder "User name"})
   (dom/on "keydown" (e/fn [e]
                       (when (= "Enter" (.-key e))
                         (when-some [v (contrib.str/empty->nil (-> e .-target .-value))]
                           (new F v)    ;'new' is used to call electric objects/functions?
                           (set! (.-value dom/node) "")))))))

(e/defn UserCreate []
  (e/client
   (UserSubmit. (e/fn [v]
                  (e/server
                   (queries/create-account !xtdb v))
                  #_(e/server
                     (e/discard
                      (xt/submit-tx !xtdb [[:xtdb.api/put
                                            {:xt/id (random-uuid)
                                             :user/name v}]])))))))

#?(:clj
   (defn user-records [db]
     (->> #_(xt/q db '{:find [(pull ?e [:xt/id :user/name])]
                     :where [[?e :user/name]]})
          (queries/get-all-accounts (xt/db user/!xtdb))
          (map first)
          (sort-by :account/name)
          vec)))

(e/defn User-list []
  (e/server
   (binding [!xtdb user/!xtdb
             db (new (db/latest-db> user/!xtdb))]
     (e/client
      (dom/link (dom/props {:rel :stylesheet :href "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css"}))
      (dom/h1 (dom/text "Planning Poker Users"))
      (dom/div
       (dom/text "Enter username:")
       (UserCreate.)
       (dom/div
        (dom/style {:padding-top "10px"})
        (dom/p
         (dom/strong 
          (dom/text "Logged in users:")))
        (e/server
         (e/for-by :xt/id [{:keys [xt/id]} (e/offload #(user-records db))]
                   (UserItem. id)))))))))
