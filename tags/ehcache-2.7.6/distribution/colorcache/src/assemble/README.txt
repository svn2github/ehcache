How to run this sample
------------------------------

NOTE: Windows users please use the equivalent Batch scripts

NOTE: JAVA_HOME environment variable must be set to valid path of JDK 1.6+ installation prior to running the scripts below.

1. Start Terracotta server first
     bin/start-sample-server.sh
     
2. Start sample: bin/start-sample.sh

3. Access colorcache sample at 
  -- http://localhost:9081/colorcache
  -- http://localhost:9082/colorcache
  
4. Shut down sample: bin/stop-sample.sh 

How to monitor cache usage with the Terracotta Management Console
------------------------------

With the sample running, you can monitor the runtime statistics
of the cache using the Terracotta Management console.

See http://terracotta.org/documentation/4.0/tms/tms for more information.