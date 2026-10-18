@echo off
setlocal
rem Reuse the module launcher with the root reactor's wrapper configuration.
set "MAVEN_BASEDIR=%~dp0."
call "%~dp0core\mvnw.cmd" %*
exit /b %ERRORLEVEL%
