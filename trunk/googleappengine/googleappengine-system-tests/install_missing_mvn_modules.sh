#!/bin/sh

#
# This script installs the Google App Engine artifacts into you local Maven repository.
#
# If you would like to avoid the need for this step, ask Google by voting for the following issue:
#   http://code.google.com/p/googleappengine/issues/detail?id=1296
#

if [ "$1" == "" ]
then
    echo "usage: $0 /path/to/appengine-java-sdk-1.3.7"
    exit 1
fi

GAE_SDK_PATH=$1

mvn install:install-file -Dfile=$GAE_SDK_PATH/lib/user/appengine-api-1.0-sdk-1.3.7.jar -DgroupId=com.google.appengine -DartifactId=appengine-api-1.0-sdk -Dversion=1.3.7 -DgeneratePom=true -Dpackaging=jar

mvn install:install-file -Dfile=$GAE_SDK_PATH/lib/user/appengine-api-labs-1.3.7.jar -DgroupId=com.google.appengine -DartifactId=appengine-api-labs -Dversion=1.3.7 -DgeneratePom=true -Dpackaging=jar

mvn install:install-file -Dfile=$GAE_SDK_PATH/lib/impl/appengine-api-stubs.jar -DgroupId=com.google.appengine -DartifactId=appengine-api-stubs -Dversion=1.3.7 -DgeneratePom=true -Dpackaging=jar

mvn install:install-file -Dfile=$GAE_SDK_PATH/lib/testing/appengine-testing.jar -DgroupId=com.google.appengine -DartifactId=appengine-testing -Dversion=1.3.7 -DgeneratePom=true -Dpackaging=jar

mvn install:install-file -Dfile=$GAE_SDK_PATH/lib/user/orm/datanucleus-appengine-1.0.7.final.jar -DgroupId=com.google.appengine -DartifactId=datanucleus-appengine -Dversion=1.0.7 -DgeneratePom=true -Dpackaging=jar

mvn install:install-file -Dfile=$GAE_SDK_PATH/lib/user/orm/geronimo-jta_1.1_spec-1.1.1.jar -DgroupId=com.google.appengine -DartifactId=geronimo-jta_1.1_spec -Dversion=1.1.1 -DgeneratePom=true -Dpackaging=jar
