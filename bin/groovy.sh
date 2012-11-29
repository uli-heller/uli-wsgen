#!/bin/sh

D="$(dirname "$0")"
D="$(cd "${D}"; pwd)"

if [ ! -d "${D}/groovy" ]; then
  "${D}/prepare.sh"
fi

CLASSPATH="${D}/../build/classes:${CLASSPATH}"
export CLASSPATH

GROOVY_HOME="${D}/groovy"
export GROOVY_HOME

exec "${GROOVY_HOME}/bin/groovy" "$@"
