#!/usr/bin/env bash

export SERVICE_HOME=${SERVICE_HOME:-/data/app/bmr-flink-manager-api-1.0.0}
export SERVICE_LOG_DIR=${SERVICE_LOG_DIR:-/data/log}


export SERVICE_INIT_HEAP_SIZE=" -Xms4g -Xmx4g "


export CMS_GC_OPTIONS=" -server -XX:-UseBiasedLocking -XX:+UnlockDiagnosticVMOptions \
  -XX:+ParallelRefProcEnabled -XX:+CMSParallelInitialMarkEnabled -XX:+CMSScavengeBeforeRemark -XX:+PrintCommandLineFlags \
  -XX:+PrintTenuringDistribution -XX:+PrintPromotionFailure -XX:MaxTenuringThreshold=3 -XX:+UseConcMarkSweepGC \
  -XX:ErrorFile=${SERVICE_LOG_DIR}/hs_err_pid%p.log \
  -Xloggc:${SERVICE_LOG_DIR}/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps \
  -XX:+PrintGCDateStamps -XX:CMSInitiatingOccupancyFraction=75 -XX:+UseCMSInitiatingOccupancyOnly "


export SERVICE_OPTS=" ${CMS_GC_OPTIONS} ${SERVICE_INIT_HEAP_SIZE} "


CLASSPATH=$SERVICE_CONF_DIR
for  jar in $(ls $SERVICE_HOME/lib/*.jar)
do
CLASSPATH=$CLASSPATH:$jar
done

export SERVICE_CLASSPATH="${CLASSPATH}"
