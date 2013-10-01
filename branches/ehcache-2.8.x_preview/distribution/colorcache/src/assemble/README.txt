How to run this sample
------------------------------

NOTE: Windows users please use the equivalent Batch scripts

NOTE: JAVA_HOME environment variable must be set to valid path of JRE 1.5+ installation prior to running the scripts below.

1. Start Terracotta server first
     bin/start-sample-server.sh
     
2. Start sample: bin/start-sample.sh

3. Access colorcache sample at 
  -- http://localhost:9081/colorcache
  -- http://localhost:9082/colorcache
  
4. Shut down sample: bin/stop-sample.sh 

*) To use Maven to run this sample:

mvn tc:start
mvn -P9081 jetty:run-war&
mvn -P9082 jetty:run-war&

*) To use Maven to stop the Terracotta Server and sample clients:

mvn tc:stop
mvn -P9081 jetty:stop
mvn -P9082 jetty:stop

You can obtain Maven here: http://maven.apache.org/download.html

How to monitor cache usage with the Terracotta Developer Console
------------------------------

With the sample running, you can monitor the runtime statistics
of the cache using the Terracotta Developer console.

1.  Start the Terracotta Developer Console
     bin/start-developer-console.sh

2.  Connect to the running Terracotta server by clicking "Connect"

3.  Click on the Ehcache tab to view live Ehcache statistics
