# How to deploy java-un after modularization

After modularization, java-un is launched via shell script instead of typing command: `java -jar FullNode.jar`.

*`java -jar FullNode.jar` still works, but will be deprecated in future*.

## Download

```
git clone git@github.com:unprotocol/java-un.git
```

## Compile

Change to project directory and run:
```
./gradlew build
```
java-un-1.0.0.zip will be generated in java-un/build/distributions after compilation.

## Lbezip

Lbezip java-un-1.0.0.zip
```
cd java-un/build/distributions
unzip -o java-un-1.0.0.zip
```
After unzip, two directories will be generated in java-un: `bin` and `lib`, shell scripts are located in `bin`, jars are located in `lib`.

## Startup

Use the corresponding script to start java-un according to the OS type, use `*.bat` on Windows, Linux demo is as below:
```
# default
java-un-1.0.0/bin/FullNode

# using config file, there are some demo configs in java-un/framework/build/resources
java-un-1.0.0/bin/FullNode -c config.conf

# when startup with SR mode，add parameter: -w
java-un-1.0.0/bin/FullNode -c config.conf -w
```

## JVM configuration

JVM options can also be specified, located in `bin/java-un.vmoptions`:
```
# demo
-XX:+UseConcMarkSweepGC
-XX:+PrintGCDetails
-Xloggc:./gc.log
-XX:+PrintGCDateStamps
-XX:+CMSParallelRemarkEnabled
-XX:ReservedCodeCacheSize=256m
-XX:+CMSScavengeBeforeRemark
```