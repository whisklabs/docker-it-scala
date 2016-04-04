#!/bin/bash

# Internal options, always specified
INTERNAL_OPTS="-Dfile.encoding=UTF-8 -Xss4M -Xmx1024M -XX:ReservedCodeCacheSize=128M -XX:+CMSClassUnloadingEnabled -XX:+UseConcMarkSweepGC"

# Default options, if nothing is specified
DEFAULT_OPTS=""

SBT_VERSION="0.13.11"
SBT_LAUNCHER="$(dirname $0)/project/sbt-launch-$SBT_VERSION.jar"

if [ ! -e "$SBT_LAUNCHER" ];
then
    URL="http://repo.typesafe.com/typesafe/ivy-releases/org.scala-sbt/sbt-launch/$SBT_VERSION/sbt-launch.jar"
    # curl -o $SBT_LAUNCHER $URL
    curl --fail --silent --location "$URL" --output "$SBT_LAUNCHER"
fi

# Call with INTERNAL_OPTS followed by SBT_OPTS (or DEFAULT_OPTS). java aways takes the last option when duplicate.
exec java ${INTERNAL_OPTS} ${SBT_OPTS:-${DEFAULT_OPTS}} -jar $SBT_LAUNCHER "$@"
