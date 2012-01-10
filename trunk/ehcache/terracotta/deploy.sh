#!/bin/bash

if [ "$1" = "help" ]; then
  echo "Syntax: $0 [snapshot|staging|release]"
  exit 1
fi

if [ "$1" = "" ]; then
  echo "Syntax: $0 [snapshot|staging|release]"
  exit 1
fi

mode=${1}

echo "---------------------------"
echo " DEPLOYING MODE $mode      "
echo "---------------------------"

case "$mode" in
  snapshot)
    mvn clean deploy
  ;;

  staging)
    mvn clean deploy
  ;;

  release)
    mvn clean deploy -P release
    mvn clean deploy -P deploy-sourceforge
  ;;

  *) 
    echo "deploy mode is unknown: $mode"
    exit 1
  ;;
esac

