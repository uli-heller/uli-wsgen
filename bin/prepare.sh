#!/bin/sh
#set -x
D="$(dirname "$0")"
D="$(cd "${D}"; pwd)"

TPD="${D}/../3rd-party"

GROOVY_VERSION="2.1.0-rc-3"
GROOVY_ZIP_BASENAME="groovy-binary-${GROOVY_VERSION}.zip"
GROOVY_ZIP_DOWNLOAD_URL="http://dist.groovy.codehaus.org/distributions/${GROOVY_ZIP_BASENAME}"
GROOVY_ZIP_ABSOLUTE_PATH="${TPD}/${GROOVY_ZIP_BASENAME}"

if [ ! -d "${TPD}" ]; then
  mkdir -p "${TPD}"
fi

if [ ! -d "${D}/groovy" ]; then
  if [ ! -s  "${GROOVY_ZIP_ABSOLUTE_PATH}" ]; then
    "${D}/httpcat.sh" "${GROOVY_ZIP_DOWNLOAD_URL}" >"${GROOVY_ZIP_ABSOLUTE_PATH}"
  fi
  (cd "${D}"; "${D}/myjar.sh" -xf "${GROOVY_ZIP_ABSOLUTE_PATH}")
  mv "${D}/groovy-${GROOVY_VERSION}" "${D}/groovy"
  (cd "${D}/groovy/bin"; ls|grep -v "bat"|xargs chmod +x)
fi

WSDLDIFF_JAR_BASENAME=soa-model-core-1.2.1.uli02.jar
WSDLDIFF_JAR_DOWNLOAD_URL="https://github.com/uli-heller/soa-model-core/raw/jars/${WSDLDIFF_JAR_BASENAME}"
#WSDLDIFF_ZIP_DOWNLOAD_URL="http://mirror.predic8.com/membrane/soa-model/soa-model-distribution-1.2.1.RC2.zip"
WSDLDIFF_JAR_ABSOLUTE_PATH="${TPD}/${WSDLDIFF_JAR_BASENAME}"
if [ ! -s  "${WSDLDIFF_JAR_ABSOLUTE_PATH}" ]; then
  "${D}/httpcat.sh" "${WSDLDIFF_JAR_DOWNLOAD_URL}" >"${WSDLDIFF_JAR_ABSOLUTE_PATH}"
fi
if [ ! -d "${D}/../lib" ]; then
  mkdir "${D}/../lib"
fi
cp -u "${WSDLDIFF_JAR_ABSOLUTE_PATH}" "${D}/../lib/."
exit 0
