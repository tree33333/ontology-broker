#!/bin/bash

dir=`pwd`;
cp=
for i in `find ${dir}/lib/ -iname "*.jar"` ; do
        cp=${cp}:${i}
done
echo "export CLASSPATH=${dir}/src${cp}" 
