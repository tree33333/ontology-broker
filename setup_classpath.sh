#!/bin/bash

dir=`pwd`;
cp=
for i in `find . -iname "${dir}/lib/*.jar"` ; do
        cp=${cp}:${i}
done
echo "export CLASSPATH=${dir}/src${cp}" 
