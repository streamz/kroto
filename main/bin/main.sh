#!/usr/bin/env bash

JCMD=
KROTO_HOME=../target/scala-2.11
APP_ARGS=
GROUP=
REP=
REPS=
START=
WORK=
EXT=
PID=
URI=
ENDPOINT=
T_PORT=
LOG_LEVEL="info"
JAVA_HOME=
APP_JAR="kroto-main.jar"

while [ "$1" != "" ]; do
    PARAM=`echo $1 | awk -F= '{print $1}'`
    VALUE=`echo $1 | awk -F= '{print $2}'`
    case ${PARAM} in
        -debug)
            JAVA_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,address=${VALUE},server=y,suspend=y ${JAVA_OPTS}"
            ;;
        -jmx)
            JMX="-Dcom.sun.management.jmxremote.port=${VALUE} -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false"
            ;;
        -telnetPort)
            T_PORT="${VALUE}"
            ;;
        -uri)
            URI=`echo ${1:5}`
            ;;
        -group)
            GROUP="${VALUE}"
            ;;
         -replicaSets)
            REPS="${VALUE}"
            ;;
        -service)
            ENDPOINT="${VALUE}"
            ;;
        -loglevel)
            LOG_LEVEL="${VALUE}"
            ;;
        *)

        echo "ERROR: unknown parameter \"$PARAM\""
        usage
        exit 1
        ;;
    esac
    shift
done

# DO CHECKS
function checkEnvironment()
{
    if [ -z "${KROTO_HOME}" ]; then
        usage
        echo "ERROR: KROTO_HOME is not set"
        exit 1
    fi

    WORK="${KROTO_HOME}/"$(hostname)."${T_PORT}"
    PID="${WORK}/kroto.${T_PORT}.pid"
    LOG_OUT="${KROTO_HOME}/logs/${T_PORT}/kroto.out"
    LOG_ERR="${KROTO_HOME}/logs/${T_PORT}/kroto.err"

    echo "[shell] Setting work dir to ${WORK}..."
    echo "[shell] Setting PID to ${PID}..."
}

function checkStartFlags()
{
    if [ -z "${T_PORT}" ]; then
        usage
        echo "ERROR: No telnet port specified"
        exit 1
    fi
    if [ -z "${GROUP}" ]; then
        usage
        echo "ERROR: No group name specified"
        exit 1
    fi
    if [ -z "${URI}" ]; then
        usage
        echo "ERROR: No uri specified"
        exit 1
    fi
    if [ -z "${REPS}" ]; then
        usage
        echo "ERROR: No replica set specified"
        exit 1
    fi
}

function run()
{
    checkEnvironment
    checkStartFlags

    APP_ARGS="-e ${ENDPOINT} -g ${GROUP} -r ${REPS} -p ${T_PORT} -u ${URI}"
    JAVA_OPTS=" ${JAVA_OPTS} -Djava.net.preferIPv4Stack=true -Dorg.slf4j.simpleLogger.defaultLogLevel=${LOG_LEVEL}"
    JAVA_OPTS="-server -Xmx1g -Xms1g ${JAVA_OPTS}"
    JCMD="${JAVA_OPTS} ${JMX} -jar ${KROTO_HOME}/${APP_JAR} ${APP_ARGS}"
    echo ${JCMD}
    java ${JCMD}
}

run