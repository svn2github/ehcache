@echo off

if "%1" == "tc_install_dir" (
  echo ../../../..
  goto end
)

if "%1" == "ehcache_jars_dir" (
  echo ../../..
  goto end
)

echo unknown param

:end
