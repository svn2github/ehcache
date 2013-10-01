#!/bin/sh
#
# All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
#

if test "$#" != "0"; then
   echo "Usage:"
   echo "  $0"
   exit 1
fi

root=`dirname $0`/..
tc_install_dir=$root/bin/`$root/bin/relative-paths.sh tc_install_dir`
exec $tc_install_dir/bin/dev-console.sh&
