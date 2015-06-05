# ビルド / 利用手順書

## ファイルについて

- jubatusonyarn

  ソースコード（sbt プロジェクト）

  - jubatus-on-yarn-application-master

    ApplicationMaster プロジェクト（juba*_proxy と1対1で起動し、Container を管理します）

  - jubatus-on-yarn-client

    メインプロジェクト（ApplicationMaster を管理します）

  - jubatus-on-yarn-common

    共通プロジェクト

  - jubatus-on-yarn-container

    Container プロジェクト（juba* と1対1で起動します）

  - jubatus-on-yarn-test

    結合試験用プロジェクト

以降のコマンド実行は jubatusonyarn ディレクトリ以下で行うこととします。


## 実行環境

- Scientific Linux 6.5
- Cloudera CDH5 / Hadoop 2.3.0
- Jubatus 0.6.2
- マスター1台、ノード3台

### 実行環境の YARN の設定

※すべてのノードで行います。

yarn-site.xml の yarn.nodemanager.remote-app-log-dir をコメントアウトします。

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

hadoop グループに現在のユーザーを追加します。

```
$ sudo gpasswd -a ＜ユーザー＞ hadoop
```

node-manager を再起動してください。

```
$ sudo service hadoop-yarn-nodemanager restart
```

### 実行に必要なファイル

実行には HDFS 上に次のファイルが必要です。

- /jubatus-on-yarn/application-master/jubatus-on-yarn-application-master.jar
- /jubatus-on-yarn/application-master/entrypoint.sh
- /jubatus-on-yarn/container/jubatus-on-yarn-container.jar
- /jubatus-on-yarn/container/entrypoint.sh

※ basePath（/jubatus-on-yarn）は start 時に指定可能です。


### 実行に必要なファイルの準備

- application-master/jubatus-on-yarn-application-master.jar

  jubatus-on-yarn-application-master のビルド結果を HDFS 上へコピーします。

    ```
    $ ./sbt "project jubatus-on-yarn-application-master" assembly
    $ hadoop fs -copyFromLocal \
    ./jubatus-on-yarn-application-master/target/scala-2.10/jubatus-on-yarn-application-master1.1.jar \
    /jubatus-on-yarn/application-master/jubatus-on-yarn-application-master.jar
    ```

- application-master/entrypoint.sh

  jubatus-on-yarn-client プロジェクトの entrypoint.sh を HDFS 上へコピーします。

    ```
    $ hadoop fs -copyFromLocal \
    ./jubatus-on-yarn-client/src/test/resources/entrypoint.sh \
    /jubatus-on-yarn/application-master/entrypoint.sh
    ```

- container/jubatus-on-yarn-container.jar

  jubatus-on-yarn-container のビルド結果を HDFS 上へコピーします。

    ```
    $ ./sbt "project jubatus-on-yarn-container" assembly
    $ hadoop fs -copyFromLocal \
    ./jubatus-on-yarn-container/target/scala-2.10/jubatus-on-yarn-container1.1.jar \
    /jubatus-on-yarn/container/jubatus-on-yarn-container.jar
    ```

- container/entrypoint.sh

  jubatus-on-yarn-application-master プロジェクトの entrypoint.sh を HDFS 上へコピーします。

    ```
    $ hadoop fs -copyFromLocal \
    ./jubatus-on-yarn-application-master/src/test/resources/entrypoint.sh \
    /jubatus-on-yarn/container/entrypoint.sh
    ```


## サンプルアプリケーションの実行

### サンプルアプリケーションのビルド

```
$ ./sbt "project jubatus-on-yarn-test" assembly
```

### config.json を HDFS にコピー

jubatus-example を clone します。

```
$ sudo yum -y install git
$ git clone https://github.com/jubatus/jubatus-example
```

shogun.json を HDFS にコピーします。

```
$ hadoop fs -mkdir /jubatus-on-yarn/sample
$ hadoop fs -copyFromLocal ./jubatus-example/shogun/shogun.json /jubatus-on-yarn/sample/shogun.json
```

#### サンプルアプリケーションの実行

```
$ hadoop jar ./jubatus-on-yarn-test/target/scala-2.10/jubatus-on-yarn-test1.1.jar us.jubat.yarn.test.Test9
```

YARN により、いずれかのノードで jubaclassifier_proxy、jubaclassifier が起動します。

status/save/load/stop/kill のうちいずれかを入力すると、対応した処理が実行されます。

終了するには stop または kill を実行後、exit を実行してください。


## インタフェースの利用方法

インタフェースは jubatus-on-yarn-client に格納されています。

jubatus-on-yarn-client/src/main/scala/us/jubat/yarn/client/JubatusYarnApplication.scala

インタフェースの使用イメージは下記のテスト用コードを参照してください。

jubatus-on-yarn-test/src/main/scala/us/jubat/yarn/test/Test9.scala


### sbt プロジェクトから参照する方法

`./sbt publishLocal` を実行し、ローカルリポジトリを参照してください。

プロジェクトの libraryDependencies に次の依存関係を追加してください。

```
"us.jubat" %% "jubatus-on-yarn-client"  % "1.1",
"us.jubat" %% "jubatus-on-yarn-common"  % "1.1"
```

### 実行方法

scala コマンドや java コマンドで実行する場合は、`hadoop classpath` コマンドで表示されるパスを CLASSPATH に追加する必要があります。

```
$ hadoop classpath
/etc/hadoop/conf:/usr/lib/hadoop/lib/*:/usr/lib/hadoop/.//*:/usr/lib/hadoop-hdfs/./:/usr/lib/hadoop-hdfs/lib/*:/usr/lib/hadoop-hdfs/.//*:/usr/lib/hadoop-yarn/lib/*:/usr/lib/hadoop-yarn/.//*:/usr/lib/hadoop-mapreduce/lib/*:/usr/lib/hadoop-mapreduce/.//*
```


## ログの確認

YARN アプリケーションのログはアプリケーションの終了後に `yarn logs` コマンドで参照できます。

```
$ yarn application -kill [applicationId]
$ yarn logs -applicationId [applicationId] | less
```

※ applicationId はサンプルアプリケーションのログに出力されるものです。

