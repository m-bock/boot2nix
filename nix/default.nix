{ stdenv, fetchurl, boot }:
with builtins;
let
  fetchJar = x:
    fetchurl {
      url = x.repoUrl + "/" + x.jarDir + "/" + x.jarFile;
      sha1 = x.sha1;
    };
  dependencyData = (import ./deps.nix);
in
stdenv.mkDerivation {
  name = "thought2--boot2nix";
  version = "0.1.0-SNAPSHOT";
  builder = ./builder.sh;
  src = ../.;
  jarDirs = map (getAttr "jarDir") dependencyData;
  jarFiles = map (getAttr "jarFile") dependencyData;
  jars = map fetchJar dependencyData;
  inherit boot;
}
