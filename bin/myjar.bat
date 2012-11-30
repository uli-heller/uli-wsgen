::@echo off
::
:: Wrapper-Skript zum Aufruf von JAR.EXE
:: Wir benötigen hierzu ein Java Development Kit (JDK)
::
if not %JAVA_HOME%x == x goto call_jar
for /D %%f in ("%ProgramFiles%\Java\jdk*") do set JAVA_HOME=%%f
:call_jar
%JAVA_HOME%\bin\jar %*
