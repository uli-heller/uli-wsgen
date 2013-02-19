@echo off
set D=%~pd0
set TPD=%D%\..\3rd-party
set GROOVY_VERSION=2.1.1
set GROOVY_ZIP_BASENAME=groovy-binary-%GROOVY_VERSION%.zip
set GROOVY_ZIP_ABSOLUTE_PATH=%TPD%\%GROOVY_ZIP_BASENAME%
set GROOVY_ZIP_DOWNLOAD_URL=http://dist.groovy.codehaus.org/distributions/%GROOVY_ZIP_BASENAME%

if not exist "%TPD%" mkdir "%TPD%"

if exist "%D%\groovy" goto end_groovy
  if exist "%GROOVY_ZIP_ABSOLUTE_PATH%" goto end_groovy_download
    call "%D%\httpcat.bat" "%GROOVY_ZIP_DOWNLOAD_URL%" >"%GROOVY_ZIP_ABSOLUTE_PATH%"
:end_groovy_download
  call "%D%\myjar.bat" -xf "%GROOVY_ZIP_ABSOLUTE_PATH%"
  move "groovy-%GROOVY_VERSION%" "%D%\groovy" >NUL
  del "%D%\groovy\bin\groovy" >NUL
:end_groovy

set WSDLDIFF_JAR_BASENAME=soa-model-core-1.2.1.uli02.jar
set WSDLDIFF_JAR_DOWNLOAD_URL=https://github.com/uli-heller/soa-model-core/raw/jars/%WSDLDIFF_JAR_BASENAME%
:: set WSDLDIFF_ZIP_DOWNLOAD_URL="http://mirror.predic8.com/membrane/soa-model/soa-model-distribution-1.2.1.RC2.zip"
set WSDLDIFF_JAR_ABSOLUTE_PATH=%TPD%\%WSDLDIFF_JAR_BASENAME%
if exist "%WSDLDIFF_JAR_ABSOLUTE_PATH%" goto end_wsdldiff_download
  call "%D%\httpcat.bat" "%WSDLDIFF_JAR_DOWNLOAD_URL%" >"%WSDLDIFF_JAR_ABSOLUTE_PATH%"
:end_wsdldiff_download
if not exist "%D%..\lib" mkdir "%D%\..\lib"
copy "%WSDLDIFF_JAR_ABSOLUTE_PATH%" "%D%\..\lib\."

set JAXWSRI_VERSION=2.2.7
set JAXWSRI_ZIP_BASENAME=jaxws-ri-%JAXWSRI_VERSION%.zip
set JAXWSRI_ZIP_DOWNLOAD_URL=http://repo1.maven.org/maven2/com/sun/xml/ws/jaxws-ri/%JAXWSRI_VERSION%/%JAXWSRI_ZIP_BASENAME%
set JAXWSRI_ZIP_ABSOLUTE_PATH=%TPD%\%JAXWSRI_ZIP_BASENAME%
if exist "%JAXWSRI_ZIP_ABSOLUTE_PATH%" goto end_jaxwsri_download
  call "%D%\httpcat.bat" "%JAXWSRI_ZIP_DOWNLOAD_URL%" >"%JAXWSRI_ZIP_ABSOLUTE_PATH%"
:end_jaxwsri_download
if not exist "%D%..\lib" mkdir "%D%\..\lib"
if exist "%D%..\lib\jaxws-tools.jar" goto end
  call "%D%\myjar.bat" -xf "%JAXWSRI_ZIP_ABSOLUTE_PATH%"
  copy jaxws-ri\lib\*.jar "%D%..\lib" >NUL
  rmdir /Q /S jaxws-ri >NUL
:end
exit /B 0
