(ns thought2/boot2nix.core
  "Example tasks showing various approaches."
  {:boot/export-tasks true}
  (:require [boot.core :as boot :refer [deftask with-pre-wrap]]
            [boot.util :as util]
            [clojure.string :as s]
            [clojure.java.io :as io]))

(defn remove-local-base [s]
  (second (re-matches #"^^.*\.m2/repository/(.*)$" s)))

(def repo-urls (->> (:repositories (boot.core/get-env))
                    (map (juxt first (comp :url second)))
                    (into {})))

(defn split-path [path]
  (let [parts (s/split path #"/")
        [a b] (split-at (dec (count parts)) parts)]
    [(s/join "/" a) (first b)]))

(defn get-sha1 [local-path]
  (let [f (io/file (str local-path ".sha1"))]
    (when (.exists f)
      (slurp f))))

(defn get-repo [local-path]
  (let [[base fname] (split-path local-path)
        f (str base "/" "/_maven.repositories")]
    (->> (slurp f)
         s/split-lines
         (remove #(s/starts-with? % "#"))
         (map (comp rest #(re-find #"^(.*)>(.*)=$" %)))
         (filter (comp #(= % fname) first))
         first second)))

(defn gather []
  (->> (boot.pod/with-call-worker
         (boot.aether/resolve-dependency-jars ~(boot.core/get-env)))
       (map (fn [local-path]
              (let [repo (get-repo local-path)]
                {:local-path local-path
                 :path (remove-local-base local-path)
                 :sha1 (get-sha1 local-path)
                 :repo repo
                 :repo-url (repo-urls repo)
                 })))
       #_(femap remove-local-base)))

(defn mk-builder []
  (->> (gather)
       (map (fn [{:keys [path sha1 repo-url]}]
              (s/join " " [repo-url path sha1])))
       (s/join "\n")))


(deftask nixify []
  "Makes project independent of local maven repo. Instead uses local nix-store."
  (with-pre-wrap fileset
    (prn "hello nixify!")
    (spit "nix/builder.sh" (mk-builder)) 
    fileset))
