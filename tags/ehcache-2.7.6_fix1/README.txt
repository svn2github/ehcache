1. To compile:
  %> mvn install -DskipTests
  
Note: the final Ehcache jar is found under ehcache/target  


2. To build Ehcache distribution kit:

  %> cd distribution
  
  %> mvn package (build without an embedded Terracotta kit, lean and mean Ehcache kit)
  
  %> mvn package -Dtc-kit-url=http://url/to/teracotta.tar.gz  (built with Terracotta kit, offical distribution kit)
  
3. To deploy Maven central repo (via Sonatype)

  %> mvn clean deploy -P sign-artifacts,deploy-sonatype -DskipTests
  