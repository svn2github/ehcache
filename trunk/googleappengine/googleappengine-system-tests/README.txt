Instructions for Running the Sample
===================================


Dependencies
------------

1. Set up an account on Google App Engine (these are free!)

2. Create on GAE an application. Note the name.

3. Download v 1.3.7 of the GAE SDK


Setting Properties
------------------

Override the following properties in pom.xml:

<!-- Replace this with your install location for the GAE SDK -->
<gae.home>/work/java/appengine-java-sdk-1.3.7</gae.home>

<!-- Use the name here you defined in step 2 -->
<gae.application.version>gregrluckapptest</gae.application.version>


Running the Sample Locally
--------------------------

To run the sample locally in the GAE Test Environment:

mvn gae:run         
		

Running the Sample on GAE
-------------------------

