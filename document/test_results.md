# Test Description and Results

## Execution Environment

- Scientific Linux 6.5
- Cloudera CDH5 / Hadoop 2.3.0
- Jubatus 0.6.2
- 1 CDH master, 3 CDH nodes


## Test Results

### classifier (1)

#### Test Case

nodeCount is 1

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- run start -> status -> saveModel -> loadModel -> stop in that order

#### Result

Exit properly without any exception.


### classifier (2)

#### Test Case

nodeCount is 3

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 3
- run start -> status -> saveModel -> loadModel -> stop in that order

#### Result

Exit properly without any exception.


### recommender (1)

#### Test Case

nodeCount is 1

- LearningMachineName = movielens
- LearningMachineType = recommender
- configFile = jubatus-example/movielens/config.json
- nodeCount = 1
- run start -> status -> saveModel -> loadModel -> stop in that order

#### Result

Exit properly without any exception.


### recommender (2)

#### Test Case

nodeCount is 3

- LearningMachineName = movielens
- LearningMachineType = recommender
- configFile = jubatus-example/movielens/config.json
- nodeCount = 3
- run start -> status -> saveModel -> loadModel -> stop in that order

#### Result

Exit properly without any exception.


### Parallel Execution (1)

#### Test Case

Run the following cases (1), (2) in parallel.

- 1
	- LearningMachineName = shogun
	- LearningMachineType = classifier
    - configFile = jubatus-example/shogun/shogun.json
	- nodeCount = 3
	- run start -> status -> saveModel -> loadModel -> stop in that order

- 2
	- LearningMachineName = gender
	- LearningMachineType = classifier
    - configFile = jubatus-example/shogun/shogun.json
	- nodeCount = 3
	- run start -> status -> saveModel -> loadModel -> stop in that order

#### Result

Exit properly without any exception.


### Parallel Execution (2)

#### Test Case

Run the following cases (1), (2) in parallel.

- 1
	- LearningMachineName = shogun
	- LearningMachineType = classifier
    - configFile = jubatus-example/shogun/shogun.json
	- nodeCount = 3
	- run start -> status -> saveModel -> loadModel -> stop in that order

- 2
	- LearningMachineName = movielens
	- LearningMachineType = recommender
    - configFile = jubatus-example/movielens/config.json
	- nodeCount = 3
	- run start -> status -> saveModel -> loadModel -> stop in that order

#### Result

Exit properly without any exception.


### LearningMachineName (1)

#### Test Case

Ordinary instance name

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- run start -> status -> saveModel -> loadModel -> stop in that order

#### Result

Exit properly without any exception.


### LearningMachineName (2)

#### Test Case

The instance name contains hyphen and underscore

- LearningMachineName = sho-gun_
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- run start -> status -> saveModel -> loadModel -> stop in that order

#### Result

Exit properly without any exception.


### Number of Nodes (1)

#### Test Case

nodeCount is 0

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 0
- run start -> status -> saveModel -> loadModel -> stop in that order

#### Result

java.lang.IllegalArgumentException


### Number of Nodes (2)

#### Test Case

nodeCount is 1

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- run start -> status -> saveModel -> loadModel -> stop in that order

#### Result

Exit properly without any exception.


### Number of Nodes (3)

#### Test Case

nodeCount is 2

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 2
- run start -> status -> saveModel -> loadModel -> stop in that order

#### Result

Exit properly without any exception.


### Number of Nodes (4)

#### Test Case

nodeCount is 3

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 3
- run start -> status -> saveModel -> loadModel -> stop in that order

#### Result

Exit properly without any exception.


### Number of Nodes (5)

#### Test Case

nodeCount is 1000

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1000
- run start -> status -> saveModel -> loadModel -> stop in that order

#### Result

Undefined behavior

The resource-manager hangs.


### Memory Size (1)

#### Test Case

Memory is 0 MB

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- memory = 0
- nodeCount = 1
- run start -> status -> saveModel -> loadModel -> stop in that order

#### Result

java.lang.IllegalArgumentException


### Memory Size (2)

#### Test Case

Memory is 1 MB

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- memory = 1
- nodeCount = 1
- run start -> status -> saveModel -> loadModel -> stop in that order

#### Result

Exit properly without any exception. (The real memory limit is not 1 MB, but it is 1 GB, which is YARN's configuration.)


### Memory Size (3)

#### Test Case

Memory is 512 MB

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- memory = 512
- nodeCount = 1
- run start -> status -> saveModel -> loadModel -> stop in that order

#### Result

Exit properly without any exception.


### Priority (1)

#### Test Case

Priority is -21 (outside the valid nice range)

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- priority = -21
- nodeCount = 1
- run start -> status -> saveModel -> loadModel -> stop in that order

#### Result

Exit properly without any exception.

(Looking at the node-manager's log, it seems that whatever YARN priority is set, nice -n 0 bash will be used.)


### Priority (2)

#### Test Case

Priority is -20

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- priority = -20
- nodeCount = 1
- run start -> status -> saveModel -> loadModel -> stop in that order

#### Result

Exit properly without any exception.

(Looking at the node-manager's log, it seems that whatever YARN priority is set, nice -n 0 bash will be used.)


### Priority (3)

#### Test Case

Priority is 0

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- priority = 0
- nodeCount = 1
- run start -> status -> saveModel -> loadModel -> stop in that order

#### Result

Exit properly without any exception.

(Looking at the node-manager's log, it seems that whatever YARN priority is set, nice -n 0 bash will be used.)


### Priority (4)

#### Test Case

Priority is 19

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- priority = 19
- nodeCount = 1
- run start -> status -> saveModel -> loadModel -> stop in that order

#### Result

Exit properly without any exception.

(Looking at the node-manager's log, it seems that whatever YARN priority is set, nice -n 0 bash will be used.)


### Priority (5)

#### Test Case

Priority is 20

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- priority = 20
- nodeCount = 1
- run start -> status -> saveModel -> loadModel -> stop in that order

#### Result

Exit properly without any exception.

(Looking at the node-manager's log, it seems that whatever YARN priority is set, nice -n 0 bash will be used.)


### Behavior after saveModel (1)

#### Test Case

Run status after saveModel

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- run start -> saveModel -> status -> stop in that order

#### Result

Exit properly without any exception.


### Behavior after saveModel (2)

#### Test Case

Run saveModel after saveModel

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- run start -> saveModel -> saveModel -> stop in that order

#### Result

Exit properly without any exception.


### Behavior after saveModel (3)

#### Test Case

Run loadModel after saveModel

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- run start -> saveModel -> loadModel -> stop in that order

#### Result

Exit properly without any exception.


### Behavior after saveModel (4)

#### Test Case

Run stop after saveModel

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- run start -> saveModel -> stop in that order

#### Result

Exit properly without any exception.


### Behavior after saveModel (5)

#### Test Case

Run kill after saveModel

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- run start -> saveModel -> kill in that order

#### Result

Exit properly without any exception.


### Behavior after saveModel (1)

#### Test Case

Run status after saveModel

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- run start -> saveModel -> status -> stop in that order

#### Result

Exit properly without any exception.


### Behavior after loadModel (1)

#### Test Case

Run saveModel after loadModel

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- run start -> loadModel -> saveModel -> stop in that order

#### Result

Exit properly without any exception.


### Behavior after loadModel (2)

#### Test Case

Run loadModel after loadModel

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- run start -> loadModel -> loadModel -> stop in that order

#### Result

Exit properly without any exception.


### Behavior after loadModel (3)

#### Test Case

Run stop after loadModel

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- run start -> loadModel -> stop in that order

#### Result

Exit properly without any exception.


### Behavior after loadModel (4)

#### Test Case

Run kill after loadModel

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- run start -> loadModel -> kill in that order

#### Result

Exit properly without any exception.


### Behavior after stop (1)

#### Test Case

Run status after stop

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- run start -> stop -> status in that order

#### Result

java.lang.IllegalStateException


### Behavior after stop (2)

#### Test Case

Run saveModel after stop

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- run start -> stop -> saveModel in that order

#### Result

java.lang.IllegalStateException


### Behavior after stop (3)

#### Test Case

Run loadModel after stop

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- run start -> stop -> loadModel in that order

#### Result

java.lang.IllegalStateException


### Behavior after stop (4)

#### Test Case

Run stop after stop

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- run start -> stop -> stop in that order

#### Result

java.lang.IllegalStateException


### Behavior after stop (5)

#### Test Case

Run kill after stop

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- run start -> stop -> kill in that order

#### Result

java.lang.IllegalStateException


### Behavior after kill (1)

#### Test Case

Run status after kill

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- run start -> kill -> status in that order

#### Result

java.lang.IllegalStateException


### Behavior after kill (2)

#### Test Case

Run saveModel after kill

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- run start -> kill -> saveModel in that order

#### Result

java.lang.IllegalStateException


### Behavior after kill (3)

#### Test Case

Run loadModel after kill

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- run start -> kill -> loadModel in that order

#### Result

java.lang.IllegalStateException


### Behavior after kill (4)

#### Test Case

Run stop after kill

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- run start -> kill -> stop in that order

#### Result

java.lang.IllegalStateException


### Behavior after kill (5)

#### Test Case

Run kill after kill

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- run start -> kill -> kill in that order

#### Result

java.lang.IllegalStateException


### Unexpected Termination of juba*_proxy (1)

#### Test Case

Run status after unexpected termination of juba*_proxy

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- run start ->  kill juba*_proxy -> status in that order

#### Result

java.net.ConnectException: Connection refused


### Unexpected Termination of juba*_proxy (2)

#### Test Case

Run saveModel after unexpected termination of juba*_proxy

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- run start ->  kill juba*_proxy -> saveModel in that order

#### Result

java.util.concurrent.ExecutionException: dispatch.StatusCode: Unexpected response status: 400


### Unexpected Termination of juba*_proxy (3)

#### Test Case

Run loadModel after unexpected termination of juba*_proxy

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- run start ->  kill juba*_proxy -> loadModel in that order

#### Result

java.util.concurrent.ExecutionException: dispatch.StatusCode: Unexpected response status: 400


### Unexpected Termination of juba*_proxy (4)

#### Test Case

Run stop after unexpected termination of juba*_proxy

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- run start ->  kill juba*_proxy -> stop in that order

#### Result

Exit properly without any exception.


### Unexpected Termination of juba*_proxy (5)

#### Test Case

Run kill after unexpected termination of juba*_proxy

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- run start ->  kill juba*_proxy -> kill in that order

#### Result

Exit properly without any exception.



### Unexpected Termination of juba* (1)

#### Test Case

Run status after unexpected termination of juba*_proxy

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- run start ->  kill juba* -> status in that order

#### Result

org.msgpack.rpc.error.RemoteError: no server found: shogun


### Unexpected Termination of juba* (2)

#### Test Case

Run saveModel after unexpected termination of juba*_proxy

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- run start ->  kill juba* -> saveModel in that order

#### Result

java.util.concurrent.ExecutionException: dispatch.StatusCode: Unexpected response status: 400


### Unexpected Termination of juba* (3)

#### Test Case

Run loadModel after unexpected termination of juba*_proxy

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- run start ->  kill juba* -> loadModel in that order

#### Result

java.util.concurrent.ExecutionException: dispatch.StatusCode: Unexpected response status: 400


### Unexpected Termination of juba* (4)

#### Test Case

Run stop after unexpected termination of juba*_proxy

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- run start ->  kill juba* -> stop in that order

#### Result

Exit properly without any exception.


### Unexpected Termination of juba* (5)

#### Test Case

Run kill after unexpected termination of juba*_proxy

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- run start ->  kill juba* -> kill in that order

#### Result

Exit properly without any exception.


### Set Configuration

#### Test Case

Pass configuration via configString

- LearningMachineName = shogun
- LearningMachineType = classifier
- configString = contents of jubatus-example/shogun/shogun.json
- nodeCount = 1
- run start ->  saveModel -> loadModel -> stop in that order

#### Result

Exit properly without any exception.


### unlearner (1)

#### Test Case

Learn without unlearner

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- run start -> learn something for a while

#### Result

The application is killed due to YARN memory limit.


### unlearner (2)

#### Test Case

Learn with unlearner

- LearningMachineName = shogun
- LearningMachineType = classifier
- configFile = jubatus-example/shogun/shogun.json
- nodeCount = 1
- run start -> learn something for a while

#### Result

The application is not killed by YARN.


### Set basePath

#### Test Case

Set the basePath

- LearningMachineName = shogun
- LearningMachineType = classifier
- configString = contents of jubatus-example/shogun/shogun.json
- nodeCount = 1
- basePath = hdfs:///jubatus-on-yarn2
- run start ->  saveModel -> loadModel -> stop in that order

#### Result

Exit properly without any exception.
