#!/bin/sh
#set -x
D="$(dirname "$0")"
D="$(cd "${D}"; pwd)"

GROOVY_VERSION=2.0.5
GROOVY_ZIP_BASENAME=groovy-binary-${GROOVY_VERSION}.zip
GROOVY_ZIP_DOWNLOAD_URL="http://dist.groovy.codehaus.org/distributions/${GROOVY_ZIP_BASENAME}"
GROOVY_ZIP_ABSOLUTE_PATH="${D}/../3rd-party/${GROOVY_ZIP_BASENAME}"

if [ ! -d "${D}/groovy" ]; then
  if [ ! -s  "${GROOVY_ZIP_ABSOLUTE_PATH}" ]; then
    wget -c "${GROOVY_ZIP_DOWNLOAD_URL}" -O "${GROOVY_ZIP_ABSOLUTE_PATH}"
  fi
  (cd "${D}"; "${D}/myjar.sh" -xf "${GROOVY_ZIP_ABSOLUTE_PATH}")
  mv "${D}/groovy-${GROOVY_VERSION}" "${D}/groovy"
  (cd "${D}/groovy/bin"; ls|grep -v "bat"|xargs chmod +x)
fi

exit 0
