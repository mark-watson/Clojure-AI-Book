;; Datomic Local wrapper. Copyright 2024-2026 Mark Watson. All rights reserved.
;; Eclipse Public License 2.0

(ns datomic-local.core
  "Thin wrapper around Datomic Local (com.datomic/local), the free embedded
   Datalog database. Uses the Datomic client API with :datomic-local server type.

   Datomic Local runs in-process with no separate server or transactor,
   storing data on the local filesystem or in memory.

   Quick start:

     (require '[datomic-local.core :as d])

     ;; Create a client
     (def client (d/client \"my-system\"))

     ;; Create a database and connect
     (def conn (d/create-db client \"my-kb\"))

     ;; Define a schema
     (d/define-schema conn
       [{:db/ident :person/name
         :db/valueType :db.type/string
         :db/cardinality :db.cardinality/one}])

     ;; Add data (transact returns the result map directly — synchronous)
     (d/transact conn [{:person/name \"Alice\"}])

     ;; Query with Datalog
     (d/q '[:find ?n :where [?e :person/name ?n]] (d/db conn))

   Storage:
     By default, data is stored under ~/.datomic/${system}/${db-name}/.
     Override with :storage-dir in the client config."
  (:require [datomic.client.api :as d]))

;; ---------------------------------------------------------------------------
;; Client & database lifecycle
;; ---------------------------------------------------------------------------

(defn client
  "Create a Datomic Local client.

   Accepts either a full config map or a system name:

     (client {:server-type :dev-local
              :storage-dir :mem
              :system \"my-system\"})

     (client \"my-system\")

     (client \"my-system\" :storage-dir \"/custom/path\")

   The client is lightweight — it does not connect to a server.
   Databases live under ${storage-dir}/${system}/${db-name}."
  ([arg]
   (if (map? arg)
     (d/client arg)
     (d/client {:server-type :datomic-local :system arg})))
  ([system & {:keys [storage-dir]}]
   (let [config (cond-> {:server-type :datomic-local :system system}
                  storage-dir (assoc :storage-dir storage-dir))]
     (d/client config))))

(defn connect
  "Connect to an existing database. Takes a client and a db-name string."
  [client db-name]
  (d/connect client {:db-name db-name}))

(defn create-database
  "Create a database. Takes a client and a db-name string.
   Returns true on success. The database must not already exist."
  [client db-name]
  (d/create-database client {:db-name db-name}))

(defn delete-database
  "Delete a database. Takes a client and a db-name string."
  [client db-name]
  (d/delete-database client {:db-name db-name}))

(defn create-db
  "Create a database and return a connection.
   Convenience wrapper combining client creation, database creation, and connect.

   (def conn (d/create-db \"my-system\" \"my-db\"))"
  ([db-name]
   (create-db "default" db-name))
  ([system db-name]
   (let [c (client system)]
     (d/create-database c {:db-name db-name})
     (d/connect c {:db-name db-name}))))

(defn list-databases
  "List all databases in the system. Returns a collection of database names."
  [client]
  (d/list-databases client {}))

(defn db
  "Return the current database value (an immutable snapshot) from a connection.
   All queries run against a db value, not against a connection."
  [conn]
  (d/db conn))

(defn as-of
  "Return a database value as of a specific transaction time-point.
   The time-point is a transaction ID (long), typically from (:t tx-result)."
  [db time-point]
  (d/as-of db time-point))

(defn since
  "Return a database value containing only datoms added since the given
   transaction time-point."
  [db time-point]
  (d/since db time-point))

(defn history
  "Return a database value containing all assertions and retractions
   across time. Pass to q, datoms, or index-range."
  [db]
  (d/history db))

;; ---------------------------------------------------------------------------
;; Transactions
;; ---------------------------------------------------------------------------

(defn transact
  "Transact data into a Datomic database. Returns a map synchronously:

     {:db-before <db-value-before>
      :db-after  <db-value-after>
      :tx-data   [datoms-added]
      :tempids   {tempid -> resolved-eid}}

   tx-data is a vector of entity maps. Use string tempids like
   {:db/id \"alice\" ...} for entities that need to be referenced
   within the same transaction.

   Transaction operations like [:db/add eid attr val] and
   [:db/retract eid attr val] are also supported in tx-data."
  [conn tx-data]
  (d/transact conn {:tx-data tx-data}))

;; ---------------------------------------------------------------------------
;; Queries
;; ---------------------------------------------------------------------------

(defn q
  "Execute a Datalog query against a database value.

   The query is an EDN data structure:

     [:find ?var1 ?var2 ...
      :in $ ?param1 ...
      :where [clause1] [clause2] ...]

   Additional arguments after the db value become query inputs,
   accessible via :in patterns ($1, $2, etc.).

   Examples:

     ;; Find all movie titles
     (q '[:find ?title
          :where [?e :movie/title ?title]]
        db)

     ;; Parameterized: find movies by year
     (q '[:find ?title
          :in $ ?year
          :where [?e :movie/year ?year]
                 [?e :movie/title ?title]]
        db 2021)

   Returns a set of vectors (tuples) matching the :find pattern."
  [query & args]
  (apply d/q query args))

(defn pull
  "Pull entity data by entity ID using a declarative selector pattern.

   Selector examples:
     [:*]                                       — all attributes
     [:movie/title :movie/year]                 — specific attributes
     [:movie/title {:movie/director [:*]}]      — nested pull on a :ref

   Returns a map of attribute -> value, or nil if the entity doesn't exist."
  [db selector eid]
  (d/pull db selector eid))

;; ---------------------------------------------------------------------------
;; Schema helpers
;; ---------------------------------------------------------------------------

(defn define-schema
  "Define a schema by transacting a vector of attribute definitions.

   Each attribute map must include:
     :db/ident       — keyword-ident like :movie/title
     :db/valueType   — :db.type/string, :db.type/long, :db.type/ref,
                        :db.type/boolean, :db.type/instant, :db.type/float,
                        :db.type/double, :db.type/bigdec, :db.type/bigint,
                        :db.type/keyword, :db.type/uuid, :db.type/bytes
     :db/cardinality — :db.cardinality/one or :db.cardinality/many

   Optional:
     :db/doc         — documentation
     :db/unique      — :db.unique/identity or :db.unique/value
     :db/index       — true/false (adds AVET index for faster lookups)
     :db/fulltext    — true/false (full-text search index)

   Returns the transaction result map."
  [conn schema]
  (d/transact conn {:tx-data schema}))

;; ---------------------------------------------------------------------------
;; Helpers
;; ---------------------------------------------------------------------------

(defn find-entity
  "Find the entity ID for a unique attribute value.
   Returns the entity ID or nil.

   Example:
     (find-entity db :movie/title \"The Matrix\")"
  [db attr val]
  (ffirst (d/q '[:find ?e :in $ ?a ?v :where [?e ?a ?v]] db attr val)))

(defn -main
  "REPL entry point."
  []
  (println "Datomic Local wrapper loaded.")
  (println "(client \"system\")                - create a client")
  (println "(create-db \"system\" \"db\")      - create database + connect")
  (println "(define-schema conn [...])         - define attributes")
  (println "(q '[:find ...] db)                - query with Datalog")
  (println "(pull db '[*] eid)                 - pull entity data"))
