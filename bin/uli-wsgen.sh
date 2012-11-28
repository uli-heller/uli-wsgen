#!/bin/sh

D="$(dirname "$0")"

exec "${D}/groovy.sh" "${D}/../groovy-scripts/uliWsGen.groovy" \
  "$@"

