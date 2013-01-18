#!/bin/bash
set -x
rm -rf runner
mvn clean install org.ops4j:maven-pax-plugin:run -Dframework=felix
