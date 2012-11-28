#!/bin/sh
#set -x
D="$(dirname "$0")"
D="$(cd "${D}"; pwd)"

if [ ! -d "${D}/wsdldiff" ]; then
  "${D}/prepare.sh"
fi

SOA_MODEL_HOME="${D}/wsdldiff"
export SOA_MODEL_HOME

CLASSPATH="${SOA_MODEL_HOME}/lib/*:${SOA_MODEL_HOME}/bin"
export CLASSPATH

exec "${D}/groovy.sh" "${D}/../groovy-scripts/wsdlDiff.groovy" "$@"
