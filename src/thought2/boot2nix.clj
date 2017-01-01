(ns thought2.boot2nix
  {:boot/export-tasks true}
  (:require [boot.core :as boot :refer [deftask with-pre-wrap]]
            [boot.util :as util]
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

(defn remove-local-base [s]
  (second (re-matches #"^^.*\.m2/repository/(.*)$" s)))

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

(defn jar->pom [x] (s/replace-first x #"jar$" "pom")) "dsfsd.jar"

(defn is-snapshot [path]
  (some? (re-find #"SNAPSHOT.jar$" path)))

(defn decorate-local-repo-data [local-path]  
  (let [repo (get-repo-name local-path)
        jar-path (remove-local-base local-path)
        [jar-dir jar-file] (path->dir-file jar-path)] 
    {:repo-url repo
     :jar-dir jar-dir
     :jar-file jar-file
     :pom-file (jar->pom local-path)
     :sha1 (get-sha1 local-path)
     :snapshot (is-snapshot local-path)}))

(defn get-call [{:keys [repo-url jar-dir jar-file sha1]}]
  (s/join " " ["register-and-link" repo-url jar-dir jar-file sha1]))

(defn get-calls []
  (->> dependency-jars
       (map decorate-local-repo-data)
       (remove :snapshot)
       (map get-call)
       (s/join "\n")))

(defn output [path & [data]]
  (spit (str "nix/" path)
        (render-resource path data)))

(defn first-def [& xs]
  (let [save-read #(try (eval %) (catch Exception e))
        mk-sym #(symbol (str 'boot.user '/ %))]
    (->> (map (comp save-read mk-sym) xs)
         (some identity))))

(defn handler []
  (.mkdir (io/file "nix"))
  (output "default.nix" {:name (first-def 'project '+project+ '+name+)
                         :version (first-def 'version '+version+)})
  (output "builder.sh" {:calls (get-calls)}))

(defn get-dep-data []
  (->> dependency-jars
       (map decorate-local-repo-data)
       (remove :snapshot)))

(defn handler2 []
  (.mkdir (io/file "nix"))
  (output "deps.nix" {:dependencies (get-dep-data)})
  (output "default.nix" {:name (first-def 'project '+project+ '+name+)
                         :version (first-def 'version '+version+)})
  (output "builder.sh")
  (spit "nix/debug.edn" (prn-str (get-dep-data))))

(deftask boot2nix []
  (with-pre-wrap fileset
    (handler2)
    fileset))
