(def project 'thought2/boot2nix)
(def version "0.1.0-SNAPSHOT")

(set-env! :resource-paths #{"resources" "src"}
          :dependencies   '[[de.ubercode.clostache/clostache "1.4.0"]])

(task-options!
 push {:repo "clojars"}
 pom {:project     project
      :version     version
      :description "Boot Task to Generate Nix Expressions."
      :scm         {:url "https://github.com/thought2/boot2nix"}
      :license     {"Eclipse Public License"
                    "http://www.eclipse.org/legal/epl-v10.html"}})

(deftask build-install
  "Build and install the project locally."
  []
  (comp (pom) (jar) (install)))

