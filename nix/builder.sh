source $stdenv/setup

PATH=$boot/bin:$PATH

mkLink () {
    nix_path="$1"
    jar_dir="$2"
    jar_file="$3"
    mkdir -p $BOOT_LOCAL_REPO/$jar_dir
    ln -s $nix_path $BOOT_LOCAL_REPO/$jar_dir/$jar_file
}

mkLinks () {
    jars_xs=($jars)
    jarDirs_xs=($jarDirs)
    jarFiles_xs=($jarFiles)

    for i in ${!jars_xs[*]}
    do
	mkLink ${jars_xs[$i]} ${jarDirs_xs[$i]} ${jarFiles_xs[$i]}
    done
}

export BOOT_HOME=$(pwd)/boot_home
export BOOT_LOCAL_REPO=$(pwd)/repo
export BOOT_FILE=$src/build.boot

mkdir $BOOT_LOCAL_REPO
mkdir $BOOT_HOME

mkLinks

cd $src


boot build target -d $out

