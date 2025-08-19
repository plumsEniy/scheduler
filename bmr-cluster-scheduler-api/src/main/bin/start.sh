echo "start dts transfer data service"

. /etc/profile.d/jdk.sh

source ${SERVICE_CONF_DIR}/service-env.sh
MAIN_CLASS=com.bilibili.cluster.scheduler.api.ApiApplicationServer
java -cp ${SERVICE_CLASSPATH} ${SERVICE_OPTS} ${MAIN_CLASS}


