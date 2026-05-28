;; Datomic Local tests and examples. Copyright 2024-2026 Mark Watson. All rights reserved.
;; Eclipse Public License 2.0
;;
;; These tests demonstrate Datomic Local usage patterns with educational
;; printouts showing sample output from each API call.
;;
;; Run with:  lein test
;;   (all tests pass silently — use lein repl to explore interactively)
;;
;; Or run a single test with verbose output:
;;   lein test :only datomic-local.core-test/database-lifecycle-test

(ns datomic-local.core-test
  (:require [clojure.test :refer :all]
            [datomic-local.core :as d]))

;; ---------------------------------------------------------------------------
;; Test fixtures — in-memory client + fresh DB per test
;; ---------------------------------------------------------------------------

(def ^:dynamic *client* nil)

(defn once-fixture
  "Create a single in-memory client shared by all tests."
  [f]
  (println "=== Datomic Local Examples ===")
  (println "Client config: :dev-local, :storage-dir :mem (in-memory)")
  (binding [*client* (d/client {:server-type :dev-local
                                :storage-dir :mem
                                :system "test-system"})]
    (f)))

(use-fixtures :once once-fixture)

(defn fresh-conn
  "Create a fresh database with a unique name and return a connection."
  []
  (let [db-name (str (java.util.UUID/randomUUID))]
    (d/create-database *client* db-name)
    (println (str "\n--- New database: " db-name " ---"))
    (d/connect *client* db-name)))

;; ---------------------------------------------------------------------------
;; Schema shared across tests
;; ---------------------------------------------------------------------------

(def movie-schema
  [{:db/ident       :movie/title
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity
    :db/doc         "Movie title (unique)"}

   {:db/ident       :movie/year
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc         "Release year"}

   {:db/ident       :movie/genre
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Genre"}

   {:db/ident       :movie/director
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc         "Director -- ref to :person entity"}

   {:db/ident       :movie/cast
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc         "Cast members -- refs to :person entities"}

   {:db/ident       :person/name
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Person's name"}])

;; ---------------------------------------------------------------------------
;; 1. Database lifecycle
;; ---------------------------------------------------------------------------

(deftest database-lifecycle-test
  (testing "Create database, define schema, and verify connectivity"
    (let [conn (fresh-conn)]
      ;; Schema definition
      (let [tx (d/transact conn movie-schema)]
        (println "Schema defined:" (count movie-schema) "attributes")
        (println "  Attributes include:" (pr-str (map :db/ident movie-schema)))
        (is (map? tx))
        (is (contains? tx :db-before))
        (is (contains? tx :db-after)))

      ;; Verify schema was installed by querying :db.ident
      (let [db (d/db conn)]
        (let [attrs (d/q '[:find ?attr
                           :where [?e :db/ident ?attr]]
                         db)]
          (println "Installed attributes (sample):"
                   (pr-str (take 5 attrs)))
          (is (contains? (set attrs) [:movie/title]))
          (is (contains? (set attrs) [:person/name])))))))

;; ---------------------------------------------------------------------------
;; 2. Adding entities and basic queries
;; ---------------------------------------------------------------------------

(deftest basic-entities-and-queries-test
  (testing "Add entities and query them with Datalog"
    (let [conn (fresh-conn)]
      (d/transact conn movie-schema)

      (d/transact conn
        [{:movie/title "The Matrix"
          :movie/year 1999
          :movie/genre "Sci-Fi"}
         {:movie/title "Inception"
          :movie/year 2010
          :movie/genre "Sci-Fi"}
         {:movie/title "The Godfather"
          :movie/year 1972
          :movie/genre "Crime"}
         {:movie/title "Pulp Fiction"
          :movie/year 1994
          :movie/genre "Crime"}])

      (println "Added 4 movie entities")

      (let [db (d/db conn)]

        ;; Find all movie titles
        (let [titles (d/q '[:find ?title
                            :where [?e :movie/title ?title]]
                          db)]
          (println "\nQuery: find all movie titles")
          (println "  Result:" (pr-str titles))
          (println "  Type:   " (type titles))
          (is (= 4 (count titles))))

        ;; Find distinct genres
        (let [genres (d/q '[:find ?genre
                            :where [?e :movie/genre ?genre]]
                          db)]
          (println "\nQuery: find all genres")
          (println "  Result:" (pr-str genres))
          (println "  Distinct:" (pr-str (set (map first genres))))
          (is (= #{"Sci-Fi" "Crime"} (set (map first genres)))))

        ;; Filter with multiple clauses (implicit AND)
        (let [sci-fi (d/q '[:find ?title ?year
                            :where [?e :movie/title ?title]
                                   [?e :movie/year ?year]
                                   [?e :movie/genre "Sci-Fi"]]
                          db)]
          (println "\nQuery: Sci-Fi movies (title + year)")
          (println "  Result:" (pr-str sci-fi))
          (is (= 2 (count sci-fi)))
          (is (contains? (set sci-fi) ["The Matrix" 1999]))
          (is (contains? (set sci-fi) ["Inception" 2010])))

        ;; Predicate filter: movies after 2000
        (let [recent (d/q '[:find ?title
                            :where [?e :movie/title ?title]
                                   [?e :movie/year ?year]
                                   [(> ?year 2000)]]
                          db)]
          (println "\nQuery: movies after 2000 (predicate filter)")
          (println "  Datalog:  [(> ?year 2000)] — Clojure fn in query")
          (println "  Result:" (pr-str recent))
          (is (= 1 (count recent)))
          (is (contains? (set recent) ["Inception"])))))))

;; ---------------------------------------------------------------------------
;; 3. Relationships (entity references with string tempids)
;; ---------------------------------------------------------------------------

(deftest relationships-test
  (testing "Entity references -- directors, cast, and join queries"
    (let [conn (fresh-conn)]
      (d/transact conn movie-schema)

      ;; Create people and movies in one transaction using string tempids
      (let [tx (d/transact conn
                 [{:db/id "wachowski"  :person/name "Lana Wachowski"}
                  {:db/id "nolan"      :person/name "Christopher Nolan"}
                  {:db/id "reeves"     :person/name "Keanu Reeves"}
                  {:db/id "dicaprio"   :person/name "Leonardo DiCaprio"}

                  {:movie/title    "The Matrix"
                   :movie/year     1999
                   :movie/genre    "Sci-Fi"
                   :movie/director "wachowski"
                   :movie/cast     ["reeves"]}

                  {:movie/title    "Inception"
                   :movie/year     2010
                   :movie/genre    "Sci-Fi"
                   :movie/director "nolan"
                   :movie/cast     ["dicaprio"]}])]
        (println "\nString tempids resolved to entity IDs:")
        (println "  " (pr-str (:tempids tx))))

      (let [db (d/db conn)]

        ;; Find a specific movie entity by its unique title
        (let [matrix-eid (ffirst
                          (d/q '[:find ?e
                                 :where [?e :movie/title "The Matrix"]]
                               db))]
          (println "\nEntity ID for \"The Matrix\":" matrix-eid)

          ;; Navigate to director via nested pull
          (let [movie-info (d/pull db
                                   [:movie/title :movie/year
                                    {:movie/director [:person/name]}
                                    {:movie/cast [:person/name]}]
                                   matrix-eid)]
            (println "Pull nested result:")
            (println "  " (pr-str movie-info))
            (is (= "The Matrix" (:movie/title movie-info)))
            (is (= "Lana Wachowski"
                   (get-in movie-info [:movie/director :person/name])))))

        ;; Join query: find movies by director name
        (let [nolan-movies (d/q '[:find ?title
                                  :where [?p :person/name "Christopher Nolan"]
                                         [?m :movie/director ?p]
                                         [?m :movie/title ?title]]
                                db)]
          (println "\nJoin query: movies directed by Christopher Nolan")
          (println "  Datalog:" (pr-str '[:where [?p :person/name "Christopher Nolan"]
                                                [?m :movie/director ?p]
                                                [?m :movie/title ?title]]))
          (println "  Result:" (pr-str nolan-movies))
          (is (= 1 (count nolan-movies))))

        ;; Join query: actors and their movies
        (let [actor-movies (d/q '[:find ?actor-name ?movie-title
                                  :where [?p :person/name ?actor-name]
                                         [?m :movie/cast ?p]
                                         [?m :movie/title ?movie-title]]
                                db)]
          (println "\nJoin query: actors and their movies")
          (println "  Result:" (pr-str actor-movies))
          (is (= 2 (count actor-movies)))
          (is (contains? (set actor-movies) ["Keanu Reeves" "The Matrix"]))
          (is (contains? (set actor-movies) ["Leonardo DiCaprio" "Inception"])))))))

;; ---------------------------------------------------------------------------
;; 4. Parameterized queries
;; ---------------------------------------------------------------------------

(deftest parameterized-queries-test
  (testing "Queries with :in parameters -- reusable across different inputs"
    (let [conn (fresh-conn)]
      (d/transact conn movie-schema)

      (d/transact conn
        [{:movie/title "The Matrix" :movie/year 1999 :movie/genre "Sci-Fi"}
         {:movie/title "Inception"  :movie/year 2010 :movie/genre "Sci-Fi"}
         {:movie/title "The Godfather" :movie/year 1972 :movie/genre "Crime"}
         {:movie/title "Goodfellas" :movie/year 1990 :movie/genre "Crime"}])

      (let [db (d/db conn)]

        ;; Scalar parameter
        (let [movies-from-1999 (d/q '[:find ?title
                                      :in $ ?year
                                      :where [?e :movie/year ?year]
                                             [?e :movie/title ?title]]
                                    db 1999)]
          (println "\nParameterized: movies from 1999")
          (println "  Query:   [:in $ ?year ...] with ?year = 1999")
          (println "  Result:" (pr-str movies-from-1999))
          (is (= 1 (count movies-from-1999)))
          (is (contains? (set movies-from-1999) ["The Matrix"])))

        ;; Collection parameter
        (let [genre-query '[:find ?title
                            :in $ [?genre ...]
                            :where [?e :movie/genre ?genre]
                                   [?e :movie/title ?title]]
              sci-fi-films (d/q genre-query db ["Sci-Fi"])]
          (println "\nParameterized: Sci-Fi films (collection input)")
          (println "  Query:   [:in $ [?genre ...]] with ?genre = [\"Sci-Fi\"]")
          (println "  Result:" (pr-str sci-fi-films))
          (is (= 2 (count sci-fi-films))))

        ;; Tuple parameter
        (let [titles-by-year (d/q '[:find ?title
                                    :in $ [[?year ?genre]]
                                    :where [?e :movie/year ?year]
                                           [?e :movie/genre ?genre]
                                           [?e :movie/title ?title]]
                                  db [[1999 "Sci-Fi"] [1972 "Crime"]])]
          (println "\nParameterized: tuple input [[1999 \"Sci-Fi\"] [1972 \"Crime\"]]")
          (println "  Query:   [:in $ [[?year ?genre]] ...]")
          (println "  Result:" (pr-str titles-by-year))
          (is (= 2 (count titles-by-year)))
          (is (contains? (set titles-by-year) ["The Matrix"]))
          (is (contains? (set titles-by-year) ["The Godfather"])))))))

;; ---------------------------------------------------------------------------
;; 5. Pull API -- declarative entity navigation
;; ---------------------------------------------------------------------------

(deftest pull-api-test
  (testing "Pull API for retrieving entity data in various shapes"
    (let [conn (fresh-conn)]
      (d/transact conn movie-schema)

      (let [tx (d/transact conn
                 [{:db/id "nolan"    :person/name "Christopher Nolan"}
                  {:db/id "dicaprio" :person/name "Leonardo DiCaprio"}
                  {:movie/title    "Inception"
                   :movie/year     2010
                   :movie/genre    "Sci-Fi"
                   :movie/director "nolan"
                   :movie/cast     ["dicaprio"]}])
            tempids (:tempids tx)
            db (d/db conn)
            eid (ffirst (d/q '[:find ?e
                               :where [?e :movie/title "Inception"]]
                             db))]

        ;; Pull all attributes
        (let [full (d/pull db '[*] eid)]
          (println "\nPull entity with selector '[*] (all attributes)")
          (println "  Entity ID:" eid)
          (println "  Result:" (pr-str full))
          (println "  Note: :ref attrs return {:db/id N} maps")
          (is (= "Inception" (:movie/title full)))
          (is (= 2010 (:movie/year full)))
          (is (= {:db/id (get tempids "nolan")} (:movie/director full)))
          (is (contains? (set (:movie/cast full))
                         {:db/id (get tempids "dicaprio")})))

        ;; Pull with nested expansion
        (let [nested (d/pull db
                             [:movie/title :movie/year
                              {:movie/director [:person/name]}
                              {:movie/cast [:person/name]}]
                             eid)]
          (println "\nPull with nested expansion:")
          (println "  Selector:" (pr-str '[:movie/title :movie/year
                                           {:movie/director [:person/name]}
                                           {:movie/cast [:person/name]}]))
          (println "  Result:" (pr-str nested))
          (println "  Director:" (get-in nested [:movie/director :person/name]))
          (println "  Cast:" (map :person/name (:movie/cast nested)))
          (is (= "Inception" (:movie/title nested)))
          (is (= "Christopher Nolan"
                 (get-in nested [:movie/director :person/name])))
          (is (= 1 (count (:movie/cast nested))))
          (is (= "Leonardo DiCaprio"
                 (-> nested :movie/cast first :person/name))))

        ;; Pull inside query
        (let [movies-with-directors
              (d/q '[:find (pull ?e [:movie/title :movie/year
                                     {:movie/director [:person/name]}])
                     :where [?e :movie/title]]
                   db)]
          (println "\nPull inside a query (pull expression in :find clause)")
          (println "  Result:" (pr-str movies-with-directors))
          (is (= 1 (count movies-with-directors)))
          (let [result (ffirst movies-with-directors)]
            (is (= "Inception" (:movie/title result)))
            (is (= "Christopher Nolan"
                   (get-in result [:movie/director :person/name])))))))))

;; ---------------------------------------------------------------------------
;; 6. Upsert -- add or update using unique identity
;; ---------------------------------------------------------------------------

(deftest upsert-test
  (testing "Upsert behavior with :db.unique/identity"
    (let [conn (fresh-conn)]
      (d/transact conn movie-schema)

      ;; First insert
      (d/transact conn
        [{:movie/title "The Matrix"
          :movie/year 1999
          :movie/genre "Sci-Fi"}])

      (println "\nInserted \"The Matrix\" with genre \"Sci-Fi\"")

      ;; Upsert: same unique title — resolves existing entity
      (d/transact conn
        [{:movie/title "The Matrix"
          :movie/genre "Action"}])

      (println "Upserted \"The Matrix\" with genre \"Action\"")
      (println "  :movie/title is :db.unique/identity, so Datomic resolves the existing entity")
      (println "  :movie/genre is :db.cardinality/one, so new value retracts old value")

      (let [db (d/db conn)]
        (let [entities (d/q '[:find ?e
                              :where [?e :movie/title "The Matrix"]]
                            db)]
          (println "  Entity count with this title:" (count entities))
          (is (= 1 (count entities))))

        (let [eid (ffirst (d/q '[:find ?e
                                 :where [?e :movie/title "The Matrix"]]
                               db))]
          (let [genre (:movie/genre (d/pull db '[:movie/genre] eid))]
            (println "  Effective genre:" genre "(old value retracted)")
            (is (= "Action" genre))))))))

;; ---------------------------------------------------------------------------
;; 7. Aggregates and advanced Datalog
;; ---------------------------------------------------------------------------

(deftest aggregates-test
  (testing "Datalog aggregation functions: count, min, max"
    (let [conn (fresh-conn)]
      (d/transact conn movie-schema)

      (d/transact conn
        [{:movie/title "The Matrix" :movie/year 1999 :movie/genre "Sci-Fi"}
         {:movie/title "Inception"  :movie/year 2010 :movie/genre "Sci-Fi"}
         {:movie/title "The Godfather" :movie/year 1972 :movie/genre "Crime"}
         {:movie/title "Goodfellas" :movie/year 1990 :movie/genre "Crime"}])

      (let [db (d/db conn)]

        ;; Count
        (let [total (ffirst (d/q '[:find (count ?e)
                                   :where [?e :movie/title]]
                                 db))]
          (println "\nAggregate: (count ?e)")
          (println "  Query returns #{" total "} — extract with ffirst")
          (is (= 4 total)))

        ;; Group-by count
        (let [by-genre (d/q '[:find ?genre (count ?e)
                              :where [?e :movie/genre ?genre]]
                            db)]
          (println "\nAggregate: count per genre")
          (println "  Result:" (pr-str by-genre))
          (is (= #{["Sci-Fi" 2] ["Crime" 2]} (set by-genre))))

        ;; Min
        (let [oldest-year (ffirst (d/q '[:find (min ?year)
                                         :where [?e :movie/year ?year]]
                                       db))]
          (println "\nAggregate: (min ?year) =" oldest-year)
          (is (= 1972 oldest-year)))

        ;; Max + subquery pattern
        (let [newest-year (ffirst (d/q '[:find (max ?year)
                                         :where [?e :movie/year ?year]]
                                       db))
              newest (d/q '[:find ?title
                            :in $ ?year
                            :where [?e :movie/year ?year]
                                   [?e :movie/title ?title]]
                          db newest-year)]
          (println "Aggregate: (max ?year) =" newest-year)
          (println "  Newest movie:" (pr-str newest))
          (is (contains? (set newest) ["Inception"])))))))

;; ---------------------------------------------------------------------------
;; 8. Transaction operations -- retract and add directly
;; ---------------------------------------------------------------------------

(deftest transaction-operations-test
  (testing "Direct transaction operations: :db/add, :db/retract"
    (let [conn (fresh-conn)]
      (d/transact conn movie-schema)

      (d/transact conn
        [{:movie/title "Eraserhead"
          :movie/year 1977
          :movie/genre "Surrealist"}])

      (let [db (d/db conn)
            eid (ffirst (d/q '[:find ?e
                               :where [?e :movie/title "Eraserhead"]]
                             db))]

        (println "\nEntity \"Eraserhead\" (eid:" eid ")")
        (println "  Initial genre:" (:movie/genre (d/pull db '[*] eid)))

        ;; Retract
        (d/transact conn [[:db/retract eid :movie/genre "Surrealist"]])
        (let [db2 (d/db conn)]
          (println "  After [:db/retract eid :movie/genre \"Surrealist\"]")
          (println "  Genre:" (:movie/genre (d/pull db2 '[*] eid)) "(nil)")
          (is (nil? (:movie/genre (d/pull db2 '[*] eid)))))

        ;; Add
        (d/transact conn [[:db/add eid :movie/genre "Experimental"]])
        (let [db3 (d/db conn)]
          (println "  After [:db/add eid :movie/genre \"Experimental\"]")
          (println "  Genre:" (:movie/genre (d/pull db3 '[*] eid)))
          (is (= "Experimental" (:movie/genre (d/pull db3 '[*] eid)))))))))

;; ---------------------------------------------------------------------------
;; 9. Time travel -- database as-of a point in time
;; ---------------------------------------------------------------------------

(deftest time-travel-test
  (testing "Immutable history -- querying past states of the database"
    (let [conn (fresh-conn)]
      (d/transact conn movie-schema)

      (let [tx1 (d/transact conn
                  [{:movie/title "Jaws" :movie/year 1975 :movie/genre "Thriller"}])
            _   (d/transact conn
                  [{:movie/title "Star Wars" :movie/year 1977 :movie/genre "Sci-Fi"}])]

        (println "\nTime travel demo:")
        (println "  tx1: added \"Jaws\" (t =" (:t (:db-after tx1)) ")")
        (println "  tx2: added \"Star Wars\"")

        ;; Current state
        (let [current-count (ffirst (d/q '[:find (count ?e)
                                           :where [?e :movie/title]]
                                         (d/db conn)))]
          (println "  Current count:" current-count "movies (both visible)")
          (is (= 2 current-count)))

        ;; As-of tx1 — only Jaws exists
        (let [past-db (d/as-of (d/db conn) (:t (:db-after tx1)))
              past-count (ffirst (d/q '[:find (count ?e)
                                        :where [?e :movie/title]]
                                      past-db))]
          (println "  As-of tx1 count:" past-count "movie (only Jaws)")
          (is (= 1 past-count))
          (let [titles (d/q '[:find ?title
                              :where [?e :movie/title ?title]]
                            past-db)]
            (println "  Past titles:" (pr-str titles))
            (is (= #{["Jaws"]} (set titles)))))))))

;; ---------------------------------------------------------------------------
;; 10. Build facts incrementally
;; ---------------------------------------------------------------------------

(deftest incremental-fact-building-test
  (testing "Build up a knowledge base incrementally and query across it"
    (let [conn (fresh-conn)]
      (d/transact conn movie-schema)

      ;; Day 1
      (d/transact conn
        [{:movie/title "The Matrix" :movie/year 1999 :movie/genre "Sci-Fi"}
         {:movie/title "The Matrix Reloaded" :movie/year 2003 :movie/genre "Sci-Fi"}])

      (let [day1-count (ffirst (d/q '[:find (count ?e)
                                       :where [?e :movie/title]]
                                     (d/db conn)))]
        (println "\nIncremental knowledge base:")
        (println "  Day 1: added 2 Matrix films (total:" day1-count ")")
        (is (= 2 day1-count)))

      ;; Day 2
      (d/transact conn
        [{:movie/title "The Matrix Revolutions" :movie/year 2003 :movie/genre "Sci-Fi"}])

      (let [day2-count (ffirst (d/q '[:find (count ?e)
                                       :where [?e :movie/title]]
                                     (d/db conn)))]
        (println "  Day 2: added 1 more (total:" day2-count ")")
        (is (= 3 day2-count)))

      ;; Query across all facts
      (let [all-2003 (d/q '[:find ?title
                            :where [?e :movie/year 2003]
                                   [?e :movie/title ?title]]
                          (d/db conn))]
        (println "  Query: all 2003 films" (pr-str all-2003))
        (is (= #{["The Matrix Reloaded"] ["The Matrix Revolutions"]}
               (set all-2003))))

      (let [all-sci-fi (d/q '[:find ?title
                              :where [?e :movie/genre "Sci-Fi"]
                                     [?e :movie/title ?title]]
                            (d/db conn))]
        (println "  Query: all Sci-Fi films (including day-1 data):"
                 (count all-sci-fi) "films")
        (println "  Datomic never forgets — day-1 facts still queryable")
        (is (= 3 (count all-sci-fi)))
        (is (contains? (set all-sci-fi) ["The Matrix"]))))))
