@echo off
set D=%~pd0

if exist "%D%\groovy" goto skip_prepare
call "%D%\prepare.bat"

:skip_prepare
set ANT_DIR=%D%\groovy

if not "%JAVA_HOME%"x == ""x goto call_java
for /D %%f in ("%ProgramFiles%\Java\jdk*") do set JAVA_HOME=%%f

:call_java
"%JAVA_HOME%\bin\java" -jar "%ANT_DIR%\lib\ant-launcher-1.8.4.jar" %*
