#!/bin/bash
export PATH=$PATH:/sbin:/usr/sbin

### 引数
CONTAINER_JAR_NAME="${1}"
CONTAINER_JRA_MAIN_CLASS="${2}"
CONTAINER_MEMORY_SIZE="${3}"

APPLICATION_NAME="${4}"
SEQ="${5}"
APPLICATION_MASTER_ADDRESS="${6}"
APPLICATION_MASTER_PORT="${7}"

LEARNING_MACHINE_NAME="${8}"
LEARNING_MACHINE_TYPE="${9}"
ZOOKEEPER="${10}"

IP_ADDRESS=`grep $(hostname) /etc/hosts | awk '{print $1}'`
LISTEN_IF=`netstat -ie | grep -B1 ${IP_ADDRESS} | head -n1 | awk '{print $1}'`

# launch `jubatus server`
for i in `seq 10`; do
  while true; do
    JUBATUS_SERVER_PORT=`shuf -i 1025-65535 -n 1`
    if !(ss -tna4 "( sport >= :1025 )" | tail -n +2 | awk '{print $4}' | sed  s/.*:// | grep ${JUBATUS_SERVER_PORT}); then
        break
    fi
  done

  echo juba${LEARNING_MACHINE_TYPE} --zookeeper=${ZOOKEEPER} --interval_sec=10 --interval_count=0 --rpc-port=${JUBATUS_SERVER_PORT} --name=${LEARNING_MACHINE_NAME} --listen_if ${LISTEN_IF} >> /tmp/Container 2>&1
  juba${LEARNING_MACHINE_TYPE} --zookeeper=${ZOOKEEPER} --interval_sec=10 --interval_count=0 --rpc-port=${JUBATUS_SERVER_PORT} --name=${LEARNING_MACHINE_NAME} --listen_if ${LISTEN_IF} &
  JUBATUS_SERVER_PROCESS_ID=$!

  # jubatus server の起動待機
  for j in `seq 5`; do
    if !(ps ${JUBATUS_SERVER_PROCESS_ID}); then
      continue 2
    fi

    echo jubactl --zookeeper=${ZOOKEEPER} --server=juba${LEARNING_MACHINE_TYPE} --type=${LEARNING_MACHINE_TYPE} --name=${LEARNING_MACHINE_NAME} --cmd status >> /tmp/Container 2>&1
    if (jubactl --zookeeper=${ZOOKEEPER} --server=juba${LEARNING_MACHINE_TYPE} --type=${LEARNING_MACHINE_TYPE} --name=${LEARNING_MACHINE_NAME} --cmd status \
        | awk '/active '${LEARNING_MACHINE_NAME}' members:/ {flag=1; next} /active/ {flag=0} flag==1 {print}' \
        | grep "^${IP_ADDRESS}_${JUBATUS_SERVER_PORT}$"); then
      break 2
    fi
    sleep 1
  done

  ps ${JUBATUS_SERVER_PROCESS_ID} && kill ${JUBATUS_SERVER_PROCESS_ID}
done

if !(ps ${JUBATUS_SERVER_PROCESS_ID}); then
  exit 1
fi

# launch `Container`
echo $JAVA_HOME/bin/java -Xmx${CONTAINER_MEMORY_SIZE}M ${CONTAINER_JRA_MAIN_CLASS} --seq ${SEQ} \
    --application-name ${APPLICATION_NAME} --application-master-address ${APPLICATION_MASTER_ADDRESS} --application-master-port ${APPLICATION_MASTER_PORT} \
    --container-node-address ${IP_ADDRESS} --jubatus-server-port ${JUBATUS_SERVER_PORT} --jubatus-server-process-id ${JUBATUS_SERVER_PROCESS_ID} \
    --learning-machine-name ${LEARNING_MACHINE_NAME} --learning-machine-type ${LEARNING_MACHINE_TYPE} >> /tmp/Container 2>&1
$JAVA_HOME/bin/java -Xmx${CONTAINER_MEMORY_SIZE}M ${CONTAINER_JRA_MAIN_CLASS} --seq ${SEQ} \
    --application-name ${APPLICATION_NAME} --application-master-address ${APPLICATION_MASTER_ADDRESS} --application-master-port ${APPLICATION_MASTER_PORT} \
    --container-node-address ${IP_ADDRESS} --jubatus-server-port ${JUBATUS_SERVER_PORT} --jubatus-server-process-id ${JUBATUS_SERVER_PROCESS_ID} \
    --learning-machine-name ${LEARNING_MACHINE_NAME} --learning-machine-type ${LEARNING_MACHINE_TYPE} >> /tmp/Container 2>&1
EXIT_CODE=$?
ps ${JUBATUS_SERVER_PROCESS_ID} && kill ${JUBATUS_SERVER_PROCESS_ID}
exit $EXIT_CODE
