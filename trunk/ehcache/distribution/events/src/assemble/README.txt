How to run this sample
------------------------------

NOTE: Windows users please use the equivalent Batch scripts

NOTE: JAVA_HOME environment variable must be set to valid path of JDK 1.6+ installation prior to running the scripts below.

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

How to monitor cache usage with the Terracotta Management Console
------------------------------

With the sample running, you can monitor the runtime statistics
of the cache using the Terracotta Management console.

See http://terracotta.org/documentation/4.1/tms/tms for more information.