;; This module consists of an idiomatic clojure wrapper for the Jena API.

(ns jena-clj.triplestore
  (import [clojure.lang Keyword]
          [org.apache.jena.query Dataset]
          [org.apache.jena.rdf.model Model ModelFactory ResourceFactory]
          [org.apache.jena.query Query QueryFactory ResultSet QueryExecution QueryExecutionFactory ReadWrite]
          [org.apache.jena.tdb TDBFactory]))

(defmacro with-transaction
  "Custom statement. Syntax:
   (with-transaction ReadWrite/[READ|WRITE] db
     ... body ...)
  The body is wrapped in a transaction of the supplied type (read or write) that
  works inside the supplied Jena Dataset. If an exception is thrown from whithin
  the body, the transaction is aborted and the exception is thrown. Otherwise
  the last expression in the body is returned.

  WARNING: Do not return the database or its contents (e.g. Dataset, Model) from 
  this statement or the database will escape the scope of the transaction throwing
  an exception.

  WARNING2: Beware when creating a lazy sequence of the resultset. You must consume it
  fully before trying to add new triples into the triplestore. Use doall if needed."
  [dataset rw-type & body]
  `(do (assert (and (instance? Dataset ~dataset) (instance? ReadWrite ~rw-type))
               (str "Error in with-transaction: Wrong argument types: " (type ~dataset) ", " (type ~rw-type) "\n\n"))
       (.begin ~dataset ~rw-type)
       (try (let [result# (do ~@body)] (.commit ~dataset) result#)
            (catch Throwable e# (.printStackTrace e#) (.abort ~dataset) (println "abort") (throw e#)))))

(defn init-database
  "Connects to the triplestore at given filesystem path and returns a Jena
  Dataset object that represents the connection."
  [^String path]
  (TDBFactory/createDataset path))

(defn mk-string-literal
  "Creates a Jena string literal object from a string"
  [^String s] (ResourceFactory/createStringLiteral s))

(defn mk-uri
  "Creates a Jena Resource (URI) object from a string"
  [^String s] (ResourceFactory/createResource s))

(defn mk-property
  "Creates a Jena Property object from a string"
  [^String s] (ResourceFactory/createProperty s))

;; NOTE: insert-rdf, insert-model and insert triple explicitly return nil because
;;       when used in a last step of a transaction, they would return the inner
;;       model, which would escape the scope of the transaction and then cause an
;;       exception.

(defn insert-rdf
  "Takes a tdb dataset and a path to an rdf file and inserts all the triples
   from the rdf file into the triplestore"
  [^Dataset dataset ^String rdf-path]
  (.read (.getDefaultModel dataset) rdf-path)
  nil)

(defn insert-model
  "Takes a tdb dataset and a model and inserts all triples from the model
   into the triplestore"
  [^Dataset dataset ^Model model]
  (.add (.getDefaultModel dataset) model)
  nil)

(defn insert-triple
  [^Dataset dataset, subject, predicate, object]
  (.add (.getDefaultModel dataset)
        (ResourceFactory/createStatement
         subject predicate object))
  nil)

(defn resultset->seq
  "Returns a clojure lazy sequence from a Jena ResultSet"
  [^ResultSet resultset]
  (when (.hasNext resultset)
    (lazy-seq (cons (.nextSolution resultset) (resultset->seq resultset)))))

(defn- query
  "Private function, runs a query of query-type in the specified Jena dataset"
  [^Keyword query-type ^Dataset dataset, ^String sparql-query]
  (assert (#{:select :construct} query-type) "Query must be one of the following:  [:select, :construct]")
  (let [compiled-query (QueryFactory/create sparql-query)
        execution (QueryExecutionFactory/create compiled-query dataset)]
    (({:select #(resultset->seq (.execSelect %))
       :construct #(.execConstruct %)} query-type) execution)))

(defn select-query
  "Runs a SELECT query in the specified Jena dataset."
  [^Dataset dataset, ^String sparql-query]
  (query :select dataset sparql-query))

(defn construct-query
  "Runs a CONSTRUCT query in the specified Jena dataset."
  [^Dataset dataset, ^String sparql-query]
  (query :construct dataset sparql-query))
