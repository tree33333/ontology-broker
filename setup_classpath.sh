#!/bin/bash

for i in `ls lib/**/*.jar lib/*.jar`; do 
	cp=${cp}:${i}
done

export CLASSPATH=${cp}
