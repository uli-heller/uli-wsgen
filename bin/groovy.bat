@echo off
set D=%~pd0

if not exist "%D%groovy" call "%D%prepare.bat"

set CLASSPATH=%D%..\build\classes;%D%..\lib\*;%CLASSPATH%
set GROOVY_HOME=%D%groovy

call %GROOVY_HOME%\bin\groovy.bat %*
