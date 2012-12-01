@echo off
set D=%~pd0

set GROOVY_VERSION=2.0.5
set GROOVY_ZIP_BASENAME=groovy-binary-%GROOVY_VERSION%.zip
set GROOVY_ZIP_ABSOLUTE_PATH=%D%\..\3rd-party\%GROOVY_ZIP_BASENAME%
set GROOVY_ZIP_DOWNLOAD_URL=http://dist.groovy.codehaus.org/distributions/%GROOVY_ZIP_BASENAME%

if exist "%D%\groovy" goto end_groovy
  if exist "%GROOVY_ZIP_ABSOLUTE_PATH%" goto end_groovy_download
    call %D%\httpcat.bat "%GROOVY_ZIP_DOWNLOAD_URL%" >"%GROOVY_ZIP_ABSOLUTE_PATH%"
:end_groovy_download
  call "%D%\myjar.bat" -xf "%GROOVY_ZIP_ABSOLUTE_PATH%"
  move "groovy-%GROOVY_VERSION%" "%D%\groovy" >NUL
  del "%D%\groovy\bin\groovy" >NUL
:end_groovy

set WSDLDIFF_ZIP_BASENAME=soa-model-distribution-1.2.1.RC2.zip
set WSDLDIFF_ZIP_ABSOLUTE_PATH=%D%\..\3rd-party\%WSDLDIFF_ZIP_BASENAME%
if exist "%D%\wsdldiff" goto end_wsdldiff
  if exist "%WSDLDIFF_ZIP_ABSOLUTE_PATH%" goto end_wsdldiff_download
    call %D%\httpcat.bat "%WSDLDIFF_ZIP_DOWNLOAD_URL%" >"%WSDLDIFF_ZIP_ABSOLUTE_PATH%"
:end_wsdldiff_download
  mkdir "%D%\wsdldiff"
  setlocal
  cd "%D%\wsdldiff"
  call "%D%\myjar.bat" -xf "%WSDLDIFF_ZIP_ABSOLUTE_PATH%"
  endlocal
:end_wsdldiff

:end
exit /B 0
