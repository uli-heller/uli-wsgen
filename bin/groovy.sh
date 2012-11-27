#!/bin/sh

D="$(dirname "$0")"
D="$(cd "${D}"; pwd)"

if [ ! -d "${D}/groovy" ]; then
  "${D}/prepare.sh"
fi

GROOVY_DIR="${D}/groovy"

exec "${GROOVY_DIR}/bin/groovy" "$@"
