(ns thought2.boot2nix
  {:boot/export-tasks true}
  (:import java.util.Properties)
  (:require [boot.core :as boot :refer [deftask with-pre-wrap]]
            [boot.util :as util]
            [boot.pod :refer [resolve-dependency-jars]] 
            [clojure.string :as s]
            [clojure.java.io :as io]
            [clostache.parser :refer [render-resource]]))


(def repo-urls
  (let [add-alias (fn [alias repo-name m]
                    (assoc m alias (m repo-name)))]
    (->> (:repositories (boot.core/get-env))
         (map (juxt first (comp :url second)))
         (into {})
         (add-alias "maven" "maven-central")
         (add-alias "central" "maven-central"))))

(defn get-pom-options []
  (let [result (atom {})]      
    (boot/task-options!
     boot.task.built-in/pom
     (fn [opts]
       (reset! result opts)
       opts))
    @result))

(defn load-properties [path] 
  (->> (doto (Properties.)
         (.load (io/input-stream path)))
       (map (juxt #(.getKey %) #(.getValue %)))
       (into {})))

(def dependency-jars
  (boot.pod/with-call-worker
    (boot.aether/resolve-dependency-jars ~(boot.core/get-env))))

(defn dependency-vector [a b version]
  [(symbol (str a "/" b)) version])

(def extra-dependency-jars
  (let [props           (load-properties "boot.properties")
        clojure-version (get props "BOOT_CLOJURE_VERSION")
        boot-version    (get props "BOOT_VERSION")
        boot-modules    ["core" "aether" "pod" "worker"]] 
    (map str
         (resolve-dependency-jars
          {:dependencies
           (conj 
            (map #(dependency-vector "boot" % boot-version)
                 boot-modules)
            ['org.clojure/clojure clojure-version])}))))

(defn remove-local-base [s]
  (second (re-matches #"^.*\.m2/repository/(.*)$" s)))

(defn path->dir-file [path]
  (let [parts (s/split path #"/")
        [a b] (split-at (dec (count parts)) parts)]
    [(s/join "/" a) (first b)]))

(defn get-sha1 [local-path]
  (let [file (io/file (str local-path ".sha1"))]
    (when (.exists file)
      (-> (slurp file) 
          (s/split #"[ \n]")
          first))))

(defn get-repo-name [local-path]
  (let [[base fname] (path->dir-file local-path)
        possible-files [(str base "/_remote.repositories")
                        (str base "/_maven.repositories")]
        file (first (filter #(.exists (io/file %)) possible-files))]
    (when file
      (->> (slurp file)
           s/split-lines
           (remove #(s/starts-with? % "#"))
           (map (comp second #(re-find #"^.*>(.*)=$" %)))
           (some repo-urls)))))

(defn jar->pom [x]
  (s/replace-first x #"jar$" "pom"))

(defn is-snapshot [path]
  (some? (re-find #"SNAPSHOT.jar$" path)))

(defn decorate-local-repo-data [local-jar-path]
  (let [[sub-dir jar-file] (-> local-jar-path
                               remove-local-base
                               path->dir-file)] 
    {:repo-url (get-repo-name local-jar-path)
     :sub-dir  sub-dir
     :jar      {:file jar-file
                :sha1 (get-sha1 local-jar-path)}
     :pom      {:file (jar->pom jar-file)
                :sha1 (get-sha1 (jar->pom local-jar-path))} 
     :snapshot (is-snapshot local-jar-path)}))

(defn output [dir path & [data]]
  (spit (str dir "/" path)
        (render-resource path data)))

(defn get-dep-data []
  (->> (concat dependency-jars extra-dependency-jars) 
       (map decorate-local-repo-data)
       (remove :snapshot)
       (remove #(or (empty? (-> % :jar :sha1))
                    (empty? (-> % :pom :sha1))))
       distinct))

(defn handler [dir]
  (let [pom-opts (get-pom-options)]
    (.mkdir (io/file dir))
    (output dir "deps.nix"    {:dependencies (get-dep-data)})
    (output dir "default.nix" {:name         (pom-opts :project)
                               :version      (pom-opts :version)})
    (output dir "builder.sh")))

(deftask boot2nix
  "Task to generate a Nix Expression from your project dependencies."
  [d dir VAL str
   "Target Directory, defaults to 'nix'. Is created when not existing."]
  (with-pre-wrap fileset    
    (handler (or (*opts* :dir) "nix"))
    fileset))
