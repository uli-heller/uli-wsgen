#!/bin/sh
#set -x
D="$(dirname "$0")"
D="$(cd "${D}"; pwd)"

if [ ! -d "${D}/wsdldiff" ]; then
  "${D}/prepare.sh"
fi

CLASSPATH="${D}/../lib/*"
export CLASSPATH

exec "${D}/groovy.sh" "${D}/../groovy-scripts/wsdlDiff.groovy" "$@"
