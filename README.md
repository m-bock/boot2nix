# boot2nix

[![Clojars Project][1][2]]

A [boot][3] task to generate nix expressions.

## Usage

### Prerequisites

Enter the directory of an existing boot project or create a new one:

```bash
boot -d seancorfield/boot-new new -t app -n myapp
cd myapp
```

Make sure you have a `boot.properties` inside your project folder:

```bash
boot --version > boot.properties
```

Add boot2nix as a dependency inside the `build.boot` and require the task:

```clj
(set-env! :dependencies '[[thought2/boot2nix "RELEASE"]])

(require '[thought2.boot2nix :refer [boot2nix]])
```

Run the task:

```bash
boot boot2nix
```

This creates a nix expression nix/default.nix  




[1]:https://clojars.org/thought2/boot2nix/latest-version.svg
[2]:https://clojars.org/thought2/boot2nix
[3]:https://github.com/boot-clj/boot