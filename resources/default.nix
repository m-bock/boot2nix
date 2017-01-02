{ stdenv, fetchurl, boot }:
with builtins;
let
  fetch = type: data:
    let
      x = getAttr type data;
    in
    fetchurl {
      url = data.repoUrl + "/" + data.subDir + "/" + x.file;
      sha1 = x.sha1;
    };
  dependencyData = (import ./deps.nix);
in
stdenv.mkDerivation {
  name = "{{name}}";
  version = "{{version}}";
  builder = ./builder.sh;
  src = ../.;
  jars = map (fetch "jar") dependencyData;
  poms = map (fetch "pom") dependencyData;
  subDirs = map (getAttr "subDir") dependencyData;
  jarFiles = map (x: x.jar.file) dependencyData;
  pomFiles = map (x: x.pom.file) dependencyData;

  inherit boot;
}
