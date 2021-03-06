#!/bin/sh
#set -x
D="$(dirname "$0")"
D="$(cd "${D}"; pwd)"

TPD="${D}/../3rd-party"

GROOVY_VERSION="2.1.1"
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

JAXWSRI_VERSION="2.2.7"
JAXWSRI_ZIP_BASENAME="jaxws-ri-${JAXWSRI_VERSION}.zip"
JAXWSRI_ZIP_DOWNLOAD_URL="http://repo1.maven.org/maven2/com/sun/xml/ws/jaxws-ri/${JAXWSRI_VERSION}/${JAXWSRI_ZIP_BASENAME}"
JAXWSRI_ZIP_ABSOLUTE_PATH="${TPD}/${JAXWSRI_ZIP_BASENAME}"
if [ ! -s  "${JAXWSRI_ZIP_ABSOLUTE_PATH}" ]; then
  "${D}/httpcat.sh" "${JAXWSRI_ZIP_DOWNLOAD_URL}" >"${JAXWSRI_ZIP_ABSOLUTE_PATH}"
fi
if [ ! -d "${D}/../lib" ]; then
  mkdir "${D}/../lib"
fi
if [ ! -s "${D}/../lib/jaxws-tools.jar" ]; then
  (cd "${D}/../lib"; "${D}/myjar.sh" -xf "${JAXWSRI_ZIP_ABSOLUTE_PATH}")
  mv "${D}/../lib/jaxws-ri/lib/"*.jar "${D}/../lib/."
fi

exit 0
