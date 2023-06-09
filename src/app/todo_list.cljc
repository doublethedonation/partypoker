(ns app.todo-list
  (:require #?(:clj [app.xtdb-contrib :as db])
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [hyperfiddle.electric-ui4 :as ui]
            [xtdb.api #?(:clj :as :cljs :as-alias) xt]))

(e/def !xtdb)
(e/def db) ; injected database ref; Electric defs are always dynamic

(e/defn TodoItem [id]
  (e/server
    (let [e (xt/entity db id)
          status (:task/status e)]
      (e/client
        (dom/div
          (ui/checkbox
            (case status :active false, :done true)
            (e/fn [v]
              (e/server
                (e/discard
                  (xt/submit-tx !xtdb [[:xtdb.api/put
                                        {:xt/id id
                                         :task/description (:task/description e) ; repeat
                                         :task/status (if v :done :active)}]]))))
            (dom/props {:id id}))
          (dom/label (dom/props {:for id}) (dom/text (e/server (:task/description e)))))))))

(e/defn UserItem [id]
  (e/server
    (let [e (xt/entity db id)
          #_#_status (:task/status e)]
      (e/client
        (dom/div
         (dom/label (dom/props {:for id}) (dom/text (e/server (:user/name e)))))))))

(e/defn InputSubmit [F]
                                        ; Custom input control using lower dom interface for Enter handling
  (dom/input (dom/props {:placeholder "Buy milk"})
             (dom/on "keydown" (e/fn [e]
                                 (when (= "Enter" (.-key e))
                                   (when-some [v (contrib.str/empty->nil (-> e .-target .-value))]
                                     (new F v)
                                     ;;(js/console.log (name F))
                                     (set! (.-value dom/node) "")))))))

(e/defn UserSubmit [F]
  ; Custom input control using lower dom interface for Enter handling
  (dom/input (dom/props {:placeholder "User name"})
    (dom/on "keydown" (e/fn [e]
                        (when (= "Enter" (.-key e))
                          (when-some [v (contrib.str/empty->nil (-> e .-target .-value))]
                            (new F v)
                            (set! (.-value dom/node) "")))))))

(e/defn TodoCreate []
  (e/client
   (InputSubmit. (e/fn [v]
                   (e/server
                    (e/discard
                     (xt/submit-tx !xtdb [[:xtdb.api/put
                                           {:xt/id (random-uuid)
                                            :task/description v
                                            :task/status :active}]])))))))

(e/defn UserCreate []
  (e/client
   (UserSubmit. (e/fn [v]
                  (e/server
                   (e/discard
                    (xt/submit-tx !xtdb [[:xtdb.api/put
                                          {:xt/id (random-uuid)
                                           :user/name v}]])))))))

#?(:clj
   (defn todo-records [db]
     (->> (xt/q db '{:find [(pull ?e [:xt/id :task/description])]
                     :where [[?e :task/status]]})
          (map first)
          (sort-by :task/description)
          vec)))

(comment (todo-records user/db))

#?(:clj
   (defn todo-count [db]
     (count (xt/q db '{:find [?e] :in [$ ?status]
                       :where [[?e :task/status ?status]]}
              :active))))

(comment (todo-count user/db))

#?(:clj
   (defn user-records [db]
     (->> (xt/q db '{:find [(pull ?e [:xt/id :user/name])]
                     :where [[?e :user/name]]})
          (map first)
          (sort-by :user/name)
          vec)))

(e/defn Todo-list []
  (e/server
   (binding [!xtdb user/!xtdb
             db (new (db/latest-db> user/!xtdb))]
     (e/client
      (dom/link (dom/props {:rel :stylesheet :href "/todo-list.css"}))
      (dom/h1 (dom/text "Planning Poker Users"))
      (dom/div
       (dom/text "Enter username:")
       (UserCreate.)
       (dom/div
        (e/server
         (e/for-by :xt/id [{:keys [xt/id]} (e/offload #(user-records db))]
                   (UserItem. id)))))))))
