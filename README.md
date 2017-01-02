# boot2nix

[![Clojars Project][1]][2]

A [boot][3] task to generate nix expressions.

## Usage

Enter the directory of an existing boot project or create a new one:

```bash
boot -d seancorfield/boot-new new -t app -n myapp
cd myapp
```

Make sure you have a `boot.properties` inside your project folder:

```bash
boot --version > boot.properties
```

Insdie `build.boot` add the task to dependency vector and require it:

```clj
(set-env! :dependencies '[[thought2/boot2nix "RELEASE"]])

(require '[thought2.boot2nix :refer [boot2nix]])
```

Run the task:

```bash
boot boot2nix
```

This generates some files inside a `nix` sub directory. There's a default.nix which should describe in a nix expression how to build your project.



## What happens?

This starts boot with an empty local maven repository. Thus boot will try to fetch all the dependencies that are needed to build your project (including boot and clojure itself). However, the generated nix expression tries to fetch as many dependencies as possible before actually invoking boot. This makes further builds much faster since many files will be cached in the nix store.
At the moment unfortunately not all dependencies can be pre-fetched, so there will always be some downloading overhead left on the boot side.


[1]:https://clojars.org/thought2/boot2nix/latest-version.svg
[2]:https://clojars.org/thought2/boot2nix
[3]:https://github.com/boot-clj/boot