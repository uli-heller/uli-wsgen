#!/bin/sh

D="$(dirname "$0")"
D="$(cd "${D}"; pwd)"

if [ ! -d "${D}/groovy" ]; then
  "${D}/prepare.sh"
fi

ANT_DIR="${D}/groovy"

java -jar "${ANT_DIR}/lib/ant-launcher-1.8.4.jar" "$@"
