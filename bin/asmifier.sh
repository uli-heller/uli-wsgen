#!/bin/sh

D="$(dirname "$0")"

exec "${D}/java.sh" org.objectweb.asm.util.ASMifier "$@"
