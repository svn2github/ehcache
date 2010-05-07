# This script maintains a list of paths that are owned by Marketing and should not be generated with Maven. 
# It also uploads the site to ehcache-stage using a non-destructive rsync command which will not delete target paths.
# You must be connected to the VPN to run this script, otherwise the upload will timeout
* You will be prompted for a password for ehcache-stage. See IT for access.

# change directory to generated site root
cd target/site

# remove Marketing owned paths
rm index.html

# upload
rsync -v -r * ehcache-stage.terracotta.lan:/export1/ehcache.org
