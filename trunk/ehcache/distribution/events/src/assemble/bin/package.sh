#!/bin/bash

cygwin=false
if [ `uname | grep CYGWIN` ]; then
  cygwin=true
fi

if [ "$JAVA_HOME" = "" ]; then
  echo "JAVA_HOME is not defined"
  exit 1
fi

appname=events

unset CDPATH
root=`dirname $0`/..
root=`cd $root && pwd`

jetty1=$root/jetty6.1/9081
jetty2=$root/jetty6.1/9082
webapp_lib=$root/webapps/$appname/WEB-INF/lib

tc_install_dir=$root/bin/`$root/bin/relative-paths.sh tc_install_dir`
ehcache_jars_dir=$root/bin/`$root/bin/relative-paths.sh ehcache_jars_dir`

# package ehcache-core and ehcache-terracotta
cp $ehcache_jars_dir/lib/ehcache*.jar $webapp_lib

# package toolkit runtime. It could be in 2 different places depending on which kit (ehcache vs tc)
toolkit_runtime=$tc_install_dir/common/terracotta-toolkit*-runtime*.jar

if [ ! -f $toolkit_runtime ]; then
  # not found under 'common', try 'lib'
  toolkit_runtime=$ehcache_jars_dir/lib/terracotta-toolkit*-runtime*.jar
  if [ ! -f $toolkit_runtime ]; then
    echo "Couldn't locate toolkit runtime jar"
    exit 1
  fi
fi

cp $toolkit_runtime $webapp_lib

if [ $? -eq 0 ]; then
  echo "Deploying demo..."
  cp -r $root/webapps $jetty1
  cp -r $root/webapps $jetty2
  echo "Done."
  exit 0
else
  echo "Error packaging sample"
  exit 1
fi
