# テスト結果報告書

## 実行環境

- Scientific Linux 6.5
- Cloudera CDH5 / Hadoop 2.3.0
- Jubatus 0.6.2
- マスター1台、ノード3台


## テスト結果

### classifier (1)

#### テストケース

nodeCount が 1 の場合

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- start -> status -> saveModel -> loadModel -> stop を順に行う

#### 結果

例外が発生せず、正しく終了する


### classifier (2)

#### テストケース

nodeCount が 3 の場合

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 3
- start -> status -> saveModel -> loadModel -> stop を順に行う

#### 結果

例外が発生せず、正しく終了する


### recommender (1)

#### テストケース

nodeCount が 1 の場合

- LearningMachineName = movielens
- LearningMachineType = recommender
- configFile = jubatus-example/movielens/config.json
- nodeCount = 1
- start -> status -> saveModel -> loadModel -> stop を順に行う

#### 結果

例外が発生せず、正しく終了する


### recommender (2)

#### テストケース

nodeCount が 3 の場合

- LearningMachineName = movielens
- LearningMachineType = recommender
- configFile = jubatus-example/movielens/config.json
- nodeCount = 3
- start -> status -> saveModel -> loadModel -> stop を順に行う

#### 結果

例外が発生せず、正しく終了する


### 並列実行 (1)

#### テストケース

次の 1, 2 の処理を並列で行う

- 1
	- LearningMachineName = shogun
	- LearningMachineType = classifier
    - configFile = jubatus-example/shogun/shogun.json
	- nodeCount = 3
	- start -> status -> saveModel -> loadModel -> stop を順に行う

- 2
	- LearningMachineName = gender
	- LearningMachineType = classifier
    - configFile = jubatus-example/shogun/shogun.json
	- nodeCount = 3
	- start -> status -> saveModel -> loadModel -> stop を順に行う

#### 結果

例外が発生せず、正しく終了する


### 並列実行 (2)

#### テストケース

次の 1, 2 の処理を並列で行う

- 1
	- LearningMachineName = shogun
	- LearningMachineType = classifier
    - configFile = jubatus-example/shogun/shogun.json
	- nodeCount = 3
	- start -> status -> saveModel -> loadModel -> stop を順に行う

- 2
	- LearningMachineName = movielens
	- LearningMachineType = recommender
    - configFile = jubatus-example/movielens/config.json
	- nodeCount = 3
	- start -> status -> saveModel -> loadModel -> stop を順に行う

#### 結果

例外が発生せず、正しく終了する


### LearningMachineName (1)

#### テストケース

一般的なインスタンス名

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- start -> status -> saveModel -> loadModel -> stop を順に行う

#### 結果

例外が発生せず、正しく終了する


### LearningMachineName (2)

#### テストケース

インスタンス名にハイフンやアンダースコアを含む場合

- LearningMachineName = sho-gun_
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- start -> status -> saveModel -> loadModel -> stop を順に行う

#### 結果

例外が発生せず、正しく終了する


### ノード数 (1)

#### テストケース

nodeCount が 0　の場合

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 0
- start -> status -> saveModel -> loadModel -> stop を順に行う

#### 結果

java.lang.IllegalArgumentException


### ノード数 (2)

#### テストケース

nodeCount が 1　の場合

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- start -> status -> saveModel -> loadModel -> stop を順に行う

#### 結果

例外が発生せず、正しく終了する


### ノード数 (3)

#### テストケース

nodeCount が 2　の場合

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 2
- start -> status -> saveModel -> loadModel -> stop を順に行う

#### 結果

例外が発生せず、正しく終了する


### ノード数 (4)

#### テストケース

nodeCount が 3　の場合

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 3
- start -> status -> saveModel -> loadModel -> stop を順に行う

#### 結果

例外が発生せず、正しく終了する


### ノード数 (5)

#### テストケース

nodeCount が 1000　の場合

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1000
- start -> status -> saveModel -> loadModel -> stop を順に行う

#### 結果

動作不定

resource-manager がハングしていた


### メモリサイズ (1)

#### テストケース

memory が 0 MB の場合

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- memory = 0
- nodeCount = 1
- start -> status -> saveModel -> loadModel -> stop を順に行う

#### 結果

java.lang.IllegalArgumentException


### メモリサイズ (2)

#### テストケース

memory が 1 MB の場合

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- memory = 1
- nodeCount = 1
- start -> status -> saveModel -> loadModel -> stop を順に行う

#### 結果

例外が発生せず、正しく終了する

※ YARN の設定により最低 1 GB 割り当てられ動作


### メモリサイズ (3)

#### テストケース

memory が 512 MB の場合

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- memory = 512
- nodeCount = 1
- start -> status -> saveModel -> loadModel -> stop を順に行う

#### 結果

例外が発生せず、正しく終了する


### 優先度 (1)

#### テストケース

priority が -21 (nice 値の範囲外) の場合

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- priority = -21
- nodeCount = 1
- start -> status -> saveModel -> loadModel -> stop を順に行う

#### 結果

例外が発生せず、正しく終了する

※ node-manager のログを見る限り、YARN の priority に何を指定しても、nice -n 0 bash で起動している


### 優先度 (2)

#### テストケース

priority が -20 の場合

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- priority = -20
- nodeCount = 1
- start -> status -> saveModel -> loadModel -> stop を順に行う

#### 結果

例外が発生せず、正しく終了する

※ node-manager のログを見る限り、YARN の priority に何を指定しても、nice -n 0 bash で起動している


### 優先度 (3)

#### テストケース

priority が 0 の場合

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- priority = 0
- nodeCount = 1
- start -> status -> saveModel -> loadModel -> stop を順に行う

#### 結果

例外が発生せず、正しく終了する

※ node-manager のログを見る限り、YARN の priority に何を指定しても、nice -n 0 bash で起動している


### 優先度 (4)

#### テストケース

priority が 19 の場合

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- priority = 19
- nodeCount = 1
- start -> status -> saveModel -> loadModel -> stop を順に行う

#### 結果

例外が発生せず、正しく終了する

※ node-manager のログを見る限り、YARN の priority に何を指定しても、nice -n 0 bash で起動している


### 優先度 (5)

#### テストケース

priority が 20 の場合

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- priority = 20
- nodeCount = 1
- start -> status -> saveModel -> loadModel -> stop を順に行う

#### 結果

例外が発生せず、正しく終了する

※ node-manager のログを見る限り、YARN の priority に何を指定しても、nice -n 0 bash で起動している


### saveModel 後の操作 (1)

#### テストケース

saveModel 後に status を実行

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- start -> saveModel -> status -> stop を順に行う

#### 結果

例外が発生せず、正しく終了する


### saveModel 後の操作 (２)

#### テストケース

saveModel 後に saveModel を実行

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- start -> saveModel -> saveModel -> stop を順に行う

#### 結果

例外が発生せず、正しく終了する


### saveModel 後の操作 (３)

#### テストケース

saveModel 後に loadModel を実行

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- start -> saveModel -> loadModel -> stop を順に行う

#### 結果

例外が発生せず、正しく終了する


### saveModel 後の操作 (４)

#### テストケース

saveModel 後に stop を実行

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- start -> saveModel -> stop を順に行う

#### 結果

例外が発生せず、正しく終了する


### saveModel 後の操作 (5)

#### テストケース

saveModel 後に kill を実行

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- start -> saveModel -> kill を順に行う

#### 結果

例外が発生せず、正しく終了する


### saveModel 後の操作 (6)

#### テストケース

saveModel 後に status を実行

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- start -> saveModel -> status -> stop を順に行う

#### 結果

例外が発生せず、正しく終了する


### loadModel 後の操作 (１)

#### テストケース

loadModel 後に saveModel を実行

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- start -> loadModel -> saveModel -> stop を順に行う

#### 結果

例外が発生せず、正しく終了する


### loadModel 後の操作 (２)

#### テストケース

loadModel 後に loadModel を実行

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- start -> loadModel -> loadModel -> stop を順に行う

#### 結果

例外が発生せず、正しく終了する


### loadModel 後の操作 (３)

#### テストケース

loadModel 後に stop を実行

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- start -> loadModel -> stop を順に行う

#### 結果

例外が発生せず、正しく終了する


### loadModel 後の操作 (４)

#### テストケース

loadModel 後に kill を実行

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- start -> loadModel -> kill を順に行う

#### 結果

例外が発生せず、正しく終了する


### stop 後の操作 (1)

#### テストケース

stop 後に status を実行

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- start -> stop -> status を順に行う

#### 結果

java.lang.IllegalStateException


### stop 後の操作 (2)

#### テストケース

stop 後に saveModel を実行

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- start -> stop -> saveModel を順に行う

#### 結果

java.lang.IllegalStateException


### stop 後の操作 (3)

#### テストケース

stop 後に loadModel を実行

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- start -> stop -> loadModel を順に行う

#### 結果

java.lang.IllegalStateException


### stop 後の操作 (4)

#### テストケース

stop 後に stop を実行

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- start -> stop -> stop を順に行う

#### 結果

java.lang.IllegalStateException


### stop 後の操作 (5)

#### テストケース

stop 後に kill を実行

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- start -> stop -> kill を順に行う

#### 結果

java.lang.IllegalStateException


### kill 後の操作 (1)

#### テストケース

kill 後に status を実行

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- start -> kill -> status を順に行う

#### 結果

java.lang.IllegalStateException


### kill 後の操作 (2)

#### テストケース

kill 後に saveModel を実行

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- start -> kill -> saveModel を順に行う

#### 結果

java.lang.IllegalStateException


### kill 後の操作 (3)

#### テストケース

kill 後に loadModel を実行

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- start -> kill -> loadModel を順に行う

#### 結果

java.lang.IllegalStateException


### kill 後の操作 (4)

#### テストケース

kill 後に stop を実行

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- start -> kill -> stop を順に行う

#### 結果

java.lang.IllegalStateException


### kill 後の操作 (5)

#### テストケース

kill 後に kill を実行

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- start -> kill -> kill を順に行う

#### 結果

java.lang.IllegalStateException


### juba*_proxy の異常終了 (1)

#### テストケース

juba*_proxy が異常終了後に status を実行

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- start ->  juba*_proxy を kill -> status を順に行う

#### 結果

java.net.ConnectException：接続を拒否されました


### juba*_proxy の異常終了 (2)

#### テストケース

juba*_proxy が異常終了後に saveModel を実行

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- start ->  juba*_proxy を kill -> saveModel を順に行う

#### 結果

java.util.concurrent.ExecutionException: dispatch.StatusCode: Unexpected response status: 400


### juba*_proxy の異常終了 (3)

#### テストケース

juba*_proxy が異常終了後に loadModel を実行

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- start ->  juba*_proxy を kill -> loadModel を順に行う

#### 結果

java.util.concurrent.ExecutionException: dispatch.StatusCode: Unexpected response status: 400


### juba*_proxy の異常終了 (4)

#### テストケース

juba*_proxy が異常終了後に stop を実行

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- start ->  juba*_proxy を kill -> stop を順に行う

#### 結果

例外が発生せず、正しく終了する


### juba*_proxy の異常終了 (5)

#### テストケース

juba*_proxy が異常終了後に kill を実行

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- start ->  juba*_proxy を kill -> kill を順に行う

#### 結果

例外が発生せず、正しく終了する



### juba* の異常終了 (1)

#### テストケース

juba*_proxy が異常終了後に status を実行

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- start ->  juba* を kill -> status を順に行う

#### 結果

org.msgpack.rpc.error.RemoteError: no server found: shogun


### juba* の異常終了 (2)

#### テストケース

juba*_proxy が異常終了後に saveModel を実行

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- start ->  juba* を kill -> saveModel を順に行う

#### 結果

java.util.concurrent.ExecutionException: dispatch.StatusCode: Unexpected response status: 400


### juba* の異常終了 (3)

#### テストケース

juba*_proxy が異常終了後に loadModel を実行

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- start ->  juba* を kill -> loadModel を順に行う

#### 結果

java.util.concurrent.ExecutionException: dispatch.StatusCode: Unexpected response status: 400


### juba* の異常終了 (4)

#### テストケース

juba*_proxy が異常終了後に stop を実行

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- start ->  juba* を kill -> stop を順に行う

#### 結果

例外が発生せず、正しく終了する


### juba* の異常終了 (5)

#### テストケース

juba*_proxy が異常終了後に kill を実行

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- start ->  juba* を kill -> kill を順に行う

#### 結果

例外が発生せず、正しく終了する


### config の指定

#### テストケース

configString を指定

- LearningMachineName = shogun
- LearningMachineType = classifier
- configString = jubatus-example/shogun/shogun.json の内容
- nodeCount = 1
- start ->  saveModel -> loadModel -> stop を順に行う

#### 結果

例外が発生せず、正しく終了する


### unlearner (1)

#### テストケース

unlearner を指定せずに学習を続けた場合

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- start -> 学習を続ける

#### 結果

YARN のメモリ制限によりアプリケーションが kill される


### unlearner (2)

#### テストケース

unlearner を指定して学習を続けた場合

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- start -> 学習を続ける

#### 結果

YARN により kill されることなく学習を続ける


### basePath の指定

#### テストケース

basePath を指定

- LearningMachineName = shogun
- LearningMachineType = classifier
- configString = jubatus-example/shogun/shogun.json の内容
- nodeCount = 1
- basePath = hdfs:///jubatus-on-yarn2
- start ->  saveModel -> loadModel -> stop を順に行う

#### 結果

例外が発生せず、正しく終了する
