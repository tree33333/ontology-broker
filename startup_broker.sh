#!/bin/bash

for i in lib/**/*.jar; do 
	cp=${cp}:${i}
done

export CLASSPATH=${cp}

java -Xmx200m org.sc.probro.BrokerStart