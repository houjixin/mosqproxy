#!/bin/sh

JAVA=/usr/local/java/bin/java
if [ ! -f $JAVA ]; then
	echo "Can't find java: $JAVA"
	exit -1
fi

# Build classpath (using all jar files in lib directory)
CP=.:conf
for file in lib/*
do
	if [ ! -d $file ]; then
#		file_ext=${file##*.}
#		if [ $file_ext=="jar" ]; then
		if [[ $file == *.jar ]]; then
			CP=$CP:$file
		fi
	fi
done
set PARAMS=-Djava.rmi.server.hostname=localhost -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false
$JAVA $JVM_OPTS $PARAMS -classpath $CP com.xyz.mosqproxy.starter.Starter
