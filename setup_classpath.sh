#!/bin/bash

for i in `ls lib/**/*.jar lib/*.jar`; do
        cp=${cp}:${i}
done
echo "export CLASSPATH=${cp}" | sed -e 's/=:/=/'
