#!/bin/bash
export PATH=$PATH:/sbin:/usr/sbin

### 引数
APPLICATION_MASTER_JAR_NAME="${1}"
APPLICATION_MASTER_MAIN_CLASS="${2}"
APPLICATION_MASTER_MEMORY="${3}"

APPLICATION_NAME="${4}"
MANAGEMENT_ADDRESS="${5}"
MANAGEMENT_PORT="${6}"
NODE_COUNT="${7}"
PRIORITY="${8}"
MEMORY="${9}"
VIRTUAL_CORES="${10}"
CONTAINER_MEMORY="${11}"
CONTAINER_NODES="${12}"
CONTAINER_RACKS="${13}"

LEARNING_MACHINE_NAME="${14}"
LEARNING_MACHINE_TYPE="${15}"
ZOOKEEPER="${16}"

CONFIG_FILE="${17}"

BASE_PATH="${18}"

# Server Config
SERVER_THREAD="${19}"
SERVER_TIMEOUT="${20}"
SERVER_MIXER="${21}"
SERVER_INTERVAL_SEC="${22}"
SERVER_INTERVAL_COUNT="${23}"
SERVER_ZOOKEEPER_TIMEOUT="${24}"
SERVER_INTERCONNECT_TIMEOUT="${25}"

# Proxy Config
PROXY_THREAD="${26}"
PROXY_TIMEOUT="${27}"
PROXY_ZOOKEEPER_TIMEOUT="${28}"
PROXY_INTERCONNECT_TIMEOUT="${29}"
PROXY_POOL_EXPIRE="${30}"
PROXY_POOL_SIZE="${31}"

# Log Config
APPLICATION_MASTER_LOG_CONFIG_NAME="${32}"
if [ -n "${APPLICATION_MASTER_LOG_CONFIG_NAME}" ]; then
  APPLICATION_MASTER_LOG_CONFIG_OPTION="-Dlog4j.configuration=file:${APPLICATION_MASTER_LOG_CONFIG_NAME}"
fi
JUBATUS_PROXY_LOG_CONFIG_NAME="${33}"
if [ -n "${JUBATUS_PROXY_LOG_CONFIG_NAME}" ]; then
  JUBATUS_PROXY_LOG_CONFIG_OPTION="--log_config=${JUBATUS_PROXY_LOG_CONFIG_NAME}"
fi
JUBATUS_SERVER_LOG_CONFIG_PATH="${34}"
if [ -n "${JUBATUS_SERVER_LOG_CONFIG_PATH}" ]; then
  JUBATUS_SERVER_LOG_CONFIG_PATH_OPTION="--log_config=${JUBATUS_SERVER_LOG_CONFIG_PATH}"
fi

IP_ADDRESS=`grep $(hostname) /etc/hosts | awk '{print $1}'`
LISTEN_IF=`netstat -ie | grep -B1 ${IP_ADDRESS} | head -n1 | awk '{print $1}'`

# execute `jubaconfig` command
echo jubaconfig --cmd write --zookeeper=${ZOOKEEPER} --file ${CONFIG_FILE} --name ${LEARNING_MACHINE_NAME} --type ${LEARNING_MACHINE_TYPE}
jubaconfig --cmd write --zookeeper=${ZOOKEEPER} --file ${CONFIG_FILE} --name ${LEARNING_MACHINE_NAME} --type ${LEARNING_MACHINE_TYPE}
# fail if jubaconfig failed
if [[ $? != 0 ]] ; then
    exit 1
fi

# launch `jubaclassifier_proxy`
for i in `seq 10`; do
  while true; do
    JUBATUS_PROXY_PORT=`shuf -i 1025-65535 -n 1`
    if !(ss -tna4 "( sport >= :1025 )" | tail -n +2 | awk '{print $4}' | sed  s/.*:// | grep ${JUBATUS_PROXY_PORT}); then
        break
    fi
  done

  echo juba${LEARNING_MACHINE_TYPE}_proxy --zookeeper=${ZOOKEEPER} --rpc-port=${JUBATUS_PROXY_PORT} --listen_if ${LISTEN_IF} \
    --thread ${PROXY_THREAD} --timeout ${PROXY_TIMEOUT} --zookeeper_timeout ${PROXY_ZOOKEEPER_TIMEOUT} --interconnect_timeout ${PROXY_INTERCONNECT_TIMEOUT} \
    --pool_expire ${PROXY_POOL_EXPIRE} --pool_size ${PROXY_POOL_SIZE} ${JUBATUS_PROXY_LOG_CONFIG_OPTION}
  juba${LEARNING_MACHINE_TYPE}_proxy --zookeeper=${ZOOKEEPER} --rpc-port=${JUBATUS_PROXY_PORT} --listen_if ${LISTEN_IF} \
    --thread ${PROXY_THREAD} --timeout ${PROXY_TIMEOUT} --zookeeper_timeout ${PROXY_ZOOKEEPER_TIMEOUT} --interconnect_timeout ${PROXY_INTERCONNECT_TIMEOUT} \
    --pool_expire ${PROXY_POOL_EXPIRE} --pool_size ${PROXY_POOL_SIZE} ${JUBATUS_PROXY_LOG_CONFIG_OPTION} &
  JUBATUS_PROXY_PROCESS_ID=$!

  # jubatus_proxy の起動待機
  for j in `seq 5`; do
    if !(ps ${JUBATUS_PROXY_PROCESS_ID}); then
      continue 2
    fi

    echo jubactl --zookeeper=${ZOOKEEPER} --server=juba${LEARNING_MACHINE_TYPE} --type=${LEARNING_MACHINE_TYPE} --name=${LEARNING_MACHINE_NAME} --cmd status
    if (jubactl --zookeeper=${ZOOKEEPER} --server=juba${LEARNING_MACHINE_TYPE} --type=${LEARNING_MACHINE_TYPE} --name=${LEARNING_MACHINE_NAME} --cmd status \
        | awk '/active jubaproxy members:/ {flag=1; next} /active/ {flag=0} flag==1 {print}' \
        | grep "^${IP_ADDRESS}_${JUBATUS_PROXY_PORT}$"); then
      break 2
    fi
    sleep 1
  done

  ps ${JUBATUS_PROXY_PROCESS_ID} && kill ${JUBATUS_PROXY_PROCESS_ID}
done

if !(ps ${JUBATUS_PROXY_PROCESS_ID}); then
  exit 1
fi

# launch `ApplicationMaster`
echo $JAVA_HOME/bin/java -Xmx${APPLICATION_MASTER_MEMORY}M ${APPLICATION_MASTER_LOG_CONFIG_OPTION} ${APPLICATION_MASTER_MAIN_CLASS} --application-name ${APPLICATION_NAME} --nodes ${NODE_COUNT} --priority ${PRIORITY} --memory ${MEMORY} \
  --virtual-cores ${VIRTUAL_CORES} --container-memory ${CONTAINER_MEMORY} --container-nodes "${CONTAINER_NODES}" --container-racks "${CONTAINER_RACKS}" \
  --learning-machine-name ${LEARNING_MACHINE_NAME} --learning-machine-type ${LEARNING_MACHINE_TYPE} \
  --zookeeper ${ZOOKEEPER} --management-address ${MANAGEMENT_ADDRESS} --management-port ${MANAGEMENT_PORT} \
  --application-master-node-address ${IP_ADDRESS} --jubatus-proxy-port ${JUBATUS_PROXY_PORT} --jubatus-proxy-process-id ${JUBATUS_PROXY_PROCESS_ID} \
  --base-path ${BASE_PATH} --thread ${SERVER_THREAD} --timeout ${SERVER_TIMEOUT} --mixer ${SERVER_MIXER} --interval_sec ${SERVER_INTERVAL_SEC} \
  --interval_count ${SERVER_INTERVAL_COUNT} --zookeeper_timeout ${SERVER_ZOOKEEPER_TIMEOUT} --interconnect_timeout ${SERVER_INTERCONNECT_TIMEOUT} \
  ${JUBATUS_SERVER_LOG_CONFIG_PATH_OPTION}

$JAVA_HOME/bin/java -Xmx${APPLICATION_MASTER_MEMORY}M ${APPLICATION_MASTER_LOG_CONFIG_OPTION} ${APPLICATION_MASTER_MAIN_CLASS} --application-name ${APPLICATION_NAME} --nodes ${NODE_COUNT} --priority ${PRIORITY} --memory ${MEMORY} \
  --virtual-cores ${VIRTUAL_CORES} --container-memory ${CONTAINER_MEMORY} --container-nodes "${CONTAINER_NODES}" --container-racks "${CONTAINER_RACKS}" \
  --learning-machine-name ${LEARNING_MACHINE_NAME} --learning-machine-type ${LEARNING_MACHINE_TYPE} \
  --zookeeper ${ZOOKEEPER} --management-address ${MANAGEMENT_ADDRESS} --management-port ${MANAGEMENT_PORT} \
  --application-master-node-address ${IP_ADDRESS} --jubatus-proxy-port ${JUBATUS_PROXY_PORT} --jubatus-proxy-process-id ${JUBATUS_PROXY_PROCESS_ID} \
  --base-path ${BASE_PATH} --thread ${SERVER_THREAD} --timeout ${SERVER_TIMEOUT} --mixer ${SERVER_MIXER} --interval_sec ${SERVER_INTERVAL_SEC} \
  --interval_count ${SERVER_INTERVAL_COUNT} --zookeeper_timeout ${SERVER_ZOOKEEPER_TIMEOUT} --interconnect_timeout ${SERVER_INTERCONNECT_TIMEOUT} \
  ${JUBATUS_SERVER_LOG_CONFIG_PATH_OPTION}
EXIT_CODE=$?
ps ${JUBATUS_PROXY_PROCESS_ID} && kill ${JUBATUS_PROXY_PROCESS_ID}
exit $EXIT_CODE
