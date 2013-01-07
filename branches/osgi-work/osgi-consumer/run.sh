#!/bin/bash
set -x
mvn clean install org.ops4j:maven-pax-plugin:run -Dframework=felix
