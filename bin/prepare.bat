@echo off
set D=%~pd0

set GROOVY_VERSION=2.0.5
set GROOVY_ZIP=groovy-binary-%GROOVY_VERSION%.zip

if exist "%D%\groovy" goto end_groovy
call "%D%\myjar.bat" -xf "%D%\..\3rd-party\%GROOVY_ZIP%"
move "groovy-%GROOVY_VERSION%" "%D%\groovy" >NUL
del "%D%\groovy\bin\groovy" >NUL
:end_groovy

set WSDLDIFF_ZIP_BASENAME=soa-model-distribution-1.2.1.RC2.zip
set WSDLDIFF_ZIP_ABSOLUTE_PATH=%D%\..\3rd-party\%WSDLDIFF_ZIP_BASENAME%
if exist "%D%\wsdldiff" goto end_wsdldiff
  mkdir "%D%\wsdldiff"
  setlocal
  cd "%D%\wsdldiff"
  call "%D%\myjar.bat" -xf "%WSDLDIFF_ZIP_ABSOLUTE_PATH%"
  endlocal
:end_wsdldiff

:end
exit /B 0
