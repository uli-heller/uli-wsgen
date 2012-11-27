#!/bin/sh

exec java -cp build/classes:bin/groovy/lib/* "$@"
