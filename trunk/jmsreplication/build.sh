# If the tests fail the stop_message_queues script will not be run. This script ensures that always happens. You will need to alter this script to suit
#your own install locations 

mvn -Denv.ACTIVEMQ_HOME=../../apache-activemq-5.2.0 -Denv.OPENMQ_HOME=../../mq  clean verify
# mvn will not run this if the tests fail
sh src/test/scripts/stop_message_queues

