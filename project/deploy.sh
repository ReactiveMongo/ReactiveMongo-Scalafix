#! /bin/sh

REPO="https://oss.sonatype.org/service/local/staging/deploy/maven2/"

if [ $# -lt 2 ]; then
    echo "Usage $0 version gpg-key"
    exit 1
fi

VER="$1"
KEY="$2"

echo "Password: "
read -s PASS

function deploy {
  BASE="$1"
  POM="$BASE.pom"
  FILES="$BASE.jar $BASE-javadoc.jar:javadoc $BASE-sources.jar:sources"

  for FILE in $FILES; do
    JAR=`echo "$FILE" | cut -d ':' -f 1`
    CLASSIFIER=`echo "$FILE" | cut -d ':' -f 2`

    if [ ! "$CLASSIFIER" = "$JAR" ]; then
      ARG="-Dclassifier=$CLASSIFIER"
    else
      ARG=""
    fi

    expect << EOF
set timeout 300
log_user 0
spawn mvn gpg:sign-and-deploy-file -Dkeyname=$KEY -Dpassphrase=$PASS -DpomFile=$POM -Dfile=$JAR $ARG -Durl=$REPO -DrepositoryId=sonatype-nexus-staging
log_user 1
expect "BUILD SUCCESS"
expect eof
EOF
  done
}

SCALA_MODULES="rules:reactivemongo-scalafix"
SCALA_VERSIONS="2.12 2.13"
BASES=""

for V in $SCALA_VERSIONS; do
    VERSION="$VER"

    for M in $SCALA_MODULES; do
        B=`echo "$M" | cut -d ':' -f 1`
        N=`echo "$M" | cut -d ':' -f 2`
        SCALA_DIR="$B/target/scala-$V"

        if [ ! -r "$SCALA_DIR/${N}_$V-$VERSION.pom" ]; then
            echo "Skip Scala version $V for $M"
        else
            BASES="$BASES $SCALA_DIR/$N"_$V-$VERSION
        fi
    done
done

for B in $BASES; do
  deploy "$B"
done
