@echo off

setlocal
set "CURRENT_DIR=%cd%"
set "CONF_DIR=%CURRENT_DIR%\conf"
set "ARGS= "
for /f "delims=" %%A in ('dir /b %CURRENT_DIR%\lib\*.jar') do set "JarName=%%A"
if defined  JarName   goto checkconfig
echo %CURRENT_DIR%\lib\%JarName% is not exist.Startup failed!
pause
exit

:checkconfig
if exist "%CONF_DIR%\application.yml" goto gostart
echo Configuration folder or files is not exist.Startup failed!
pause
exit

:gostart
title %JarName%
echo Startup Args:%ARGS%
echo CONF_DIR:%CONF_DIR%
set STARTUP=java -server -Dspring.config.location="%CONF_DIR%\\" -Dlogging.config="%CONF_DIR%\logback-spring.xml" %ARGS% -cp .;lib/*;lib/module/* org.springframework.boot.loader.JarLauncher
echo starting %JarName%.....
%STARTUP%
pause
exit
:end
