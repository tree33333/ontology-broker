#!/bin/bash

eval `./setup_classpath.sh`
${JAVA_HOME}/bin/java -Xmx200m org.sc.probro.BrokerStart
