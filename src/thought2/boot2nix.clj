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
  (let [add-alias #(assoc % "maven" (% "maven-central"))
        add-alias (fn [alias repo-name m]
                    (assoc m alias (m repo-name)))]
    (->> (:repositories (boot.core/get-env))
         (map (juxt first (comp :url second)))
         (into {})
         (add-alias "maven" "maven-central")
         (add-alias "central" "maven-central"))))

(def dependency-jars
  (boot.pod/with-call-worker
    (boot.aether/resolve-dependency-jars ~(boot.core/get-env))))

(def extra-dependency-jars
  (let [props (doto (Properties.)
                (.load (clojure.java.io/input-stream "boot.properties")))
        clojure-version (get props "BOOT_CLOJURE_VERSION")
        boot-version (get props "BOOT_VERSION")
        boot-modules ["core" "aether" "pod" "worker"]
        boot-deps (mapcat #(resolve-dependency-jars
                            {:dependencies [[(symbol (str "boot/" %))
                                             boot-version]]})
                          boot-modules)
        clojure-deps (resolve-dependency-jars
                      {:dependencies [['org.clojure/clojure clojure-version]]})]
    (->> (concat clojure-deps boot-deps)
         (map str))))

(defn get-pom-options []
  (let [result (atom {})]
    (boot.core/task-options!
     boot.task.built-in/pom
     (fn [opts]
       (reset! result opts)
       opts))
    @result))

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
        file (str base "/" "/_maven.repositories")]
    (->> (slurp file)
         s/split-lines
         (remove #(s/starts-with? % "#"))
         (map (comp second #(re-find #"^.*>(.*)=$" %)))
         (some repo-urls))))

(defn jar->pom [x] (s/replace-first x #"jar$" "pom"))

(defn is-snapshot [path]
  (some? (re-find #"SNAPSHOT.jar$" path)))

(defn decorate-local-repo-data [local-jar-path]
  (let [[sub-dir jar-file] (-> local-jar-path
                               remove-local-base
                               path->dir-file)] 
    {:repo-url (get-repo-name local-jar-path)
     :sub-dir sub-dir
     :jar {:file jar-file
           :sha1 (get-sha1 local-jar-path)}
     :pom {:file (jar->pom jar-file)
           :sha1 (get-sha1 (jar->pom local-jar-path))} 
     :snapshot (is-snapshot local-jar-path)}))

(defn output [path & [data]]
  (spit (str "nix/" path)
        (render-resource path data)))

(defn get-dep-data []
  (->> (concat dependency-jars extra-dependency-jars) 
       (map decorate-local-repo-data)
       (remove :snapshot)
       (remove #(or (empty? (-> % :jar :sha1))
                    (empty? (-> % :pom :sha1))))
       distinct))

(def pom-opts (get-pom-options))

(defn handler [dir]
  (.mkdir (io/file dir))
  (output (str dir "/deps.nix") {:dependencies (get-dep-data)})
  (output (str dir "/default.nix") (select-keys pom-opts [:name :version]))
  (output (str dir "/builder.sh")))

(deftask boot2nix
  "Task to generate a Nix Expression from your project dependencies."
  [d dir VAL str "Target Directory, defaults to 'nix'. Is created when not existing."]
  (with-pre-wrap fileset
    (handler (or (*opts* :dir) "nix"))
    fileset))
