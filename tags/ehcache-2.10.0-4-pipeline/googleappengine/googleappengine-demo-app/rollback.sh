#!/bin/bash

gaehome=/shares/monkeyshare/setup/tools/appengine-java-sdk-1.3.7
appname=ehcache-gae-demo

echo "This script will roll back the app that monkeys used to test $appname, not the default one in the pom"
echo "The script attempts to fix the occasional transaction error while deploying test app to Google server"

mvn clean install -Dgae.home=$gaehome -Dgae.application.name=ehcache-gae-demo $*
cd target
$gaehome/bin/appcfg.sh rollback googleappengine-demo-app-1.0-SNAPSHOT

