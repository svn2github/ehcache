How to run this sample
------------------------------

NOTE: Windows users please use the equivalent Batch scripts

1. Start Terracotta server first
     bin/start-sample-server.sh

2. Start the database:
     bin/start-db.sh

3. Start sample:
     bin/start-sample.sh

4. Access the Events sample at 
  -- http://localhost:9081/events
  -- http://localhost:9082/events
  
5. Shut down sample:
     bin/stop-db.sh
     bin/stop-sample.sh

6. Shut down the Terracotta server:
     bin/stop-sample-server.sh

*) To use Maven to start the Terracotta Server and run sample clients:

mvn tc:start&
mvn clean package
mvn -Pstart-h2 exec:java&
mvn -P9081 jetty:run-war&
mvn -P9082 jetty:run-war&

*) To use Maven to stop the Terracotta Server and sample clients:

mvn -P9081 jetty:stop
mvn -P9082 jetty:stop
mvn -Pstop-h2 exec:java
mvn tc:stop

You can obtain Maven here: http://maven.apache.org/download.html

How to monitor cache usage with the Terracotta Developer Console
------------------------------

With the sample running, you can monitor the runtime statistics
of the cache using the Terracotta Developer console.

1.  Start the Terracotta Developer Console
     bin/start-developer-console.sh
   
2.  Connect to the running Terracotta server by clicking "Connect"

3.  Click on the Ehcache tab to view live Ehcache statistics
 
