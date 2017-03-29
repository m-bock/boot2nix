# boot2nix

[![Clojars Project][1]][2]

A [Clojure Boot][3] task to generate [Nix Expressions][4].

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

You must at least have Clojure 1.8 defined in there.

Inside `build.boot` add the task to dependency vector and require it:

```clj
(set-env! :dependencies '[[thought2/boot2nix "RELEASE"]])

(require '[thought2.boot2nix :refer [boot2nix]])
```

You can alternatively put it into your `~/.boot/profile.boot`

You should have a task called `build` inside your `build.boot` which does not use the `target` task inside its task composition. Many default templates do this, including the `app` template which is used above, so you might want to remove it.

Run the task:

```bash
boot boot2nix
```
This generates some files inside a `nix` sub directory. There's a default.nix which should describe in a nix expression how to build your project.

See `boot boot2nix -h` for the available options.

You can test the result with the following nix command:

```bash
nix-build -E "with import <nixpkgs> {}; callPackage ./nix/default.nix {}"
```

When successful, this should produce a symlink called `result` in the current directory. It points to a place in the nix store which contains your build output. If you think there's more than needed inside, make use of boot's `sift` task.



## What happend behind the scenes?

This task starts boot with an empty local maven repository. Thus boot will try to fetch all the dependencies that are needed to build your project (including boot and clojure itself). However, the generated Nix expression tries to fetch as many dependencies as possible before actually invoking boot. They are put with their SHA in the nix store then, so they are cached in further builds.
At the moment unfortunately not all dependencies can be pre-fetched, so there will always be some downloading overhead left on the boot side.


[1]:https://clojars.org/thought2/boot2nix/latest-version.svg
[2]:https://clojars.org/thought2/boot2nix
[3]:https://github.com/boot-clj/boot
[4]:https://nixos.org/
