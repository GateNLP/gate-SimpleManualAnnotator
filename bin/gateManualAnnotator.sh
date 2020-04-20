#!/bin/bash

if [ "$GATE_HOME" == "" ] 
then
  echo Environment variable GATE_HOME is not set
  exit 1
fi

PRG="$0"
CURDIR="`pwd`"
# need this for relative symlinks
while [ -h "$PRG" ] ; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`"/$link"
  fi
done
SCRIPTDIR=`dirname "$PRG"`
SCRIPTDIR=`cd "$SCRIPTDIR"; pwd -P`
ROOTDIR=`cd "$SCRIPTDIR"; cd ..; pwd -P`

if [[ -f "${GATE_HOME}/gate.classpath" ]]
then
  gatecp=`cat "${GATE_HOME}/gate.classpath"`
else
  if [[ -d "${GATE_HOME}/lib" ]]
  then
    gatecp="${GATE_HOME}/lib/"'*'
  else
    echo Could not find $GATE_HOME/gate.classpath nor $GATE_HOME/lib
    exit 1
  fi
fi


java -cp ${ROOTDIR}/target/simplemanualannotator-2.0-SNAPSHOT.jar:$gatecp gate.tools.SimpleManualAnnotator "$@"

