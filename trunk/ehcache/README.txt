1. To compile:
  %> mvn install -DskipTests
  
Note: the final Ehcache jar is found under packaging/target  


2. To build Ehcache distribution kit:

  %> cd ehcache-kit
  
  %> mvn package (build without an embedded Terracotta kit, lean and mean Ehcache kit)
  
  %> mvn package -P with-tc  -Dtc-kit-url=http://url/to/teracotta.tar.gz  (built with Terracotta kit, offical distribution kit)
  
