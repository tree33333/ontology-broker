#!/bin/bash

cp=
for i in `find . -iname "*.jar"` ; do
        cp=${cp}:${i}
done
echo "export CLASSPATH=/home/tdanford/broker/src:${cp}"  | sed -e 's/::/:/'
