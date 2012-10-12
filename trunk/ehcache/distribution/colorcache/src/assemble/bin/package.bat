@echo off

if not defined JAVA_HOME (
  echo JAVA_HOME is not defined
  exit /b 1
)

setlocal

set JAVA_HOME="%JAVA_HOME:"=%"
set bin_dir="%~d0%~p0"
set root=%~d0%~p0..
set root="%root:"=%"
set jetty1=%root%\jetty6.1\9081\webapps
set jetty2=%root%\jetty6.1\9082\webapps


cd %bin_dir%
for /f "tokens=*" %%a in ('call relative-paths.bat ehcache_jars_dir') do set ehcache_jars_dir=%%a
pushd 
cd %ehcache_jars_dir%
set ehcache_jars_dir=%CD%
set ehcache_jars_dir="%ehcache_jars_dir:"=%"
popd

cd %bin_dir%
for /f "tokens=*" %%a in ('call relative-paths.bat tc_install_dir') do set tc_install_dir=%%a
pushd 
cd %tc_install_dir%
set tc_install_dir=%CD%
set tc_install_dir="%tc_install_dir:"=%"
popd

set appname=colorcache

cd %root%
set webapp_lib=webapps\%appname%\WEB-INF\lib

rem package ehcache-core and ehcache-terracotta
xcopy /y /q %ehcache_jars_dir%\lib\ehcache*.jar %webapp_lib% 1> NUL

if exist %tc_install_dir%\common\terracotta-toolkit*-runtime*.jar (
  set toolkit_runtime=%tc_install_dir%\common\terracotta-toolkit*-runtime*.jar
)

if exist %ehcache_jars_dir%\lib\terracotta-toolkit*-runtime*.jar (
  set toolkit_runtime=%ehcache_jars_dir%\lib\terracotta-toolkit*-runtime*.jar
)

if not exist %toolkit_runtime% (
  echo Couldn't locate toolkit runtime jar
  exit /b 1
)

xcopy /y /q %toolkit_runtime% %webapp_lib% 1> NUL

if %errorlevel% == 0 (
  echo Deploying demo...
  xcopy /y /q /e webapps %jetty1% 1> NUL
  xcopy /y /q /e webapps %jetty2% 1> NUL
  echo Done.
) else (
  echo Error packaging sample
  exit /b 1
)

endlocal
