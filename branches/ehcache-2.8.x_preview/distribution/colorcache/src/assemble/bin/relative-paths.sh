#!/bin/bash

case "$1" in
  tc_install_dir)
    echo ../../../..
    ;;
  ehcache_jars_dir)
    echo ../../..
    ;;
  *)
    echo "unknown param"
    exit 1
esac
