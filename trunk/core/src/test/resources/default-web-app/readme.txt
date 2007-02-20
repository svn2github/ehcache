default-web-app is an exploded directory with a structure suitable for the Otion application server.
Orion is used only for testing. No Orion files form part of the ehcache binary distribution.

default-web-app is copied to the build/orion directory when preparing to run tests. ehcache.jar, the tests, ehcache.xml
and required libraries are then copied to the default-web-app's WEB-INF/lib directory so that ehcache is available to
the default-web-app.
 
The much trimmed down Orion distribution is configured to use it as its default web application, thus the name.

Web tests rely on the contents of the default-web-app directory.

