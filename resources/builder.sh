source $stdenv/setup

PATH=$boot/bin:$PATH

mkLink () {
    jar="$1"
    pom="$2"
    subDir="$3"
    jarFile="$4"
    pomFile="$5"
    mkdir -p $BOOT_LOCAL_REPO/$subDir;
    ln -s $jar $BOOT_LOCAL_REPO/$subDir/$jarFile
    ln -s $pom $BOOT_LOCAL_REPO/$subDir/$pomFile
}

mkLinks () {
    jars_xs=($jars)
    poms_xs=($poms)
    subDirs_xs=($subDirs)
    jarFiles_xs=($jarFiles)
    pomFiles_xs=($pomFiles)

    for i in ${!jars_xs[*]}
    do
	mkLink ${jars_xs[$i]} ${poms_xs[$i]} ${subDirs_xs[$i]} ${jarFiles_xs[$i]} ${pomFiles_xs[$i]}
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

