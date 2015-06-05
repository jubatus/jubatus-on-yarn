# Build and Usage Instructions

## Files

- jubatusonyarn

  Source code (sbt project)

  - jubatus-on-yarn-application-master/

    Application Master project (1:1 relation with juba*_proxy, manages containers)

  - jubatus-on-yarn-client/

    Main project (manages the Application Master)

  - jubatus-on-yarn-common/

    Shared project

  - jubatus-on-yarn-container/

    Container project (1:1 relation with juba* instances)

  - jubatus-on-yarn-test/

    Integration test code

All of the following commands are expected to be run in the jubatusonyarn directory.


## Execution Environment

- Scientific Linux 6.5
- Cloudera CDH5 / Hadoop 2.3.0
- Jubatus 0.6.2
- 1 CDH master, 3 CDH nodes

### YARN Setup

Do the following on all nodes:

Comment out the yarn.nodemanager.remote-app-log-dir setting in yarn-site.xml:

```
$ sudo vi /etc/hadoop/conf/yarn-site.xml
<!--
	<property>
        <description>Where to aggregate logs to.</description>
        <name>yarn.nodemanager.remote-app-log-dir</name>
        <value>hdfs://var/log/hadoop-yarn/apps</value>
    </property>
-->
```

Add the current user to the hadoop group:

```
$ sudo gpasswd -a username hadoop
```

Restart the node manager:

```
$ sudo service hadoop-yarn-nodemanager restart
```

### Required Files

The following files need to exist in HDFS:

- /jubatus-on-yarn/application-master/jubatus-on-yarn-application-master.jar
- /jubatus-on-yarn/application-master/entrypoint.sh
- /jubatus-on-yarn/container/jubatus-on-yarn-container.jar
- /jubatus-on-yarn/container/entrypoint.sh

The base path (/jubatus-on-yarn) can be configured when calling the start() method.


### Creating the Required Files

- application-master/jubatus-on-yarn-application-master.jar

  Copy the build result of jubatus-on-yarn-application-master to HDFS.

    ```
    $ ./sbt "project jubatus-on-yarn-application-master" assembly
    $ hadoop fs -copyFromLocal \
    ./jubatus-on-yarn-application-master/target/scala-2.10/jubatus-on-yarn-application-master1.1.jar \
    /jubatus-on-yarn/application-master/jubatus-on-yarn-application-master.jar
    ```

- application-master/entrypoint.sh

  Copy the entrypoint.sh file from the jubatus-on-yarn-client project to HDFS.

    ```
    $ hadoop fs -copyFromLocal \
    ./jubatus-on-yarn-client/src/test/resources/entrypoint.sh \
    /jubatus-on-yarn/application-master/entrypoint.sh
    ```

- container/jubatus-on-yarn-container.jar

  Copy the build result of jubatus-on-yarn-container to HDFS.

    ```
    $ ./sbt "project jubatus-on-yarn-container" assembly
    $ hadoop fs -copyFromLocal \
    ./jubatus-on-yarn-container/target/scala-2.10/jubatus-on-yarn-container1.1.jar \
    /jubatus-on-yarn/container/jubatus-on-yarn-container.jar
    ```

- container/entrypoint.sh

  Copy the entrypoint.sh file from the jubatus-on-yarn-application-master project to HDFS.

    ```
    $ hadoop fs -copyFromLocal \
    ./jubatus-on-yarn-application-master/src/test/resources/entrypoint.sh \
    /jubatus-on-yarn/container/entrypoint.sh
    ```


## Run the Example Application

### Build the Example Application

```
$ ./sbt "project jubatus-on-yarn-test" assembly
```

### Copy the Jubatus Configuration to HDFS

Clone the jubatus-example repository.

```
$ sudo yum -y install git
$ git clone https://github.com/jubatus/jubatus-example
```

Copy the shogun.json file to HDFS.

```
$ hadoop fs -mkdir /jubatus-on-yarn/sample
$ hadoop fs -copyFromLocal ./jubatus-example/shogun/shogun.json /jubatus-on-yarn/sample/shogun.json
```

### Run the Example Application

```
$ hadoop jar ./jubatus-on-yarn-test/target/scala-2.10/jubatus-on-yarn-test1.1.jar us.jubat.yarn.test.Test9
```

Via YARN, jubaclassifier_proxy  and jubaclassifier are started.

You can enter status/save/load/stop/kill to execute the respective action.

After you stopped Jubatus with stop or kill, use exit to end the program.


### Using the Interface

The interface is contained in the jubatus-on-yarn-client package.

jubatus-on-yarn-client/src/main/scala/us/jubat/yarn/client/JubatusYarnApplication.scala

The way to use this interface can best be seen in the following file:

jubatus-on-yarn-test/src/main/scala/us/jubat/yarn/test/Test9.scala


### Reference the Code from a local sbt Project

When running `./sbt publishLocal`, the client code will be packaged and published to a local repository.

Then add the following lines to your project's libraryDependencies:

```
"us.jubat" %% "jubatus-on-yarn-client"  % "1.1",
"us.jubat" %% "jubatus-on-yarn-common"  % "1.1"
```

### How to Execute Your Program

If you want to execute your program using the scala or java command, the path shown by `hadoop classpath` must be added to the CLASSPATH:

```
$ hadoop classpath
/etc/hadoop/conf:/usr/lib/hadoop/lib/*:/usr/lib/hadoop/.//*:/usr/lib/hadoop-hdfs/./:/usr/lib/hadoop-hdfs/lib/*:/usr/lib/hadoop-hdfs/.//*:/usr/lib/hadoop-yarn/lib/*:/usr/lib/hadoop-yarn/.//*:/usr/lib/hadoop-mapreduce/lib/*:/usr/lib/hadoop-mapreduce/.//*
```

## Check the Logs

The logs of the YARN application can be obtained via `yarn logs` after the program has finished.

```
$ yarn application -kill [applicationId]
$ yarn logs -applicationId [applicationId] | less
```

