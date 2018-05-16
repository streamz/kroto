#!/usr/bin/env bash

JCMD=
KROTO_HOME=../target/scala-2.11
APP_ARGS=
DAEMON=
JMX=
NAME=
PORT=
GROUP=
PROTO=
REP=
REPS=
START=
WORK=
EXT=
PID=
ENDPOINT=
QUERY_STRING=
LOG_OUT=
LOG_ERR=
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
        -port)
            PORT="${VALUE}"
            ;;
        -tport)
            T_PORT="${VALUE}"
            ;;
        -group)
            GROUP="${VALUE}"
            ;;
        -replica)
            REP="${VALUE}"
            ;;
         -replicas)
            REPS="${VALUE}"
            ;;
        -endpoint)
            ENDPOINT="${VALUE}"
            ;;
        -proto)
            PROTO="${VALUE}"
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

    WORK="${KROTO_HOME}/"$(hostname)."${PORT}"
    PID="${WORK}/streamz.${PORT}.pid"
    LOG_OUT="${KROTO_HOME}/logs/${PORT}/kroto.out"
    LOG_ERR="${KROTO_HOME}/logs/${PORT}/kroto.err"

    echo "[shell] Setting work dir to ${WORK}..."
    echo "[shell] Setting PID to ${PID}..."
}

function checkStartFlags()
{
    if [ -z "${PORT}" ]; then
        usage
        echo "ERROR: No port specified"
        exit 1
    fi
    if [ -z "${GROUP}" ]; then
        usage
        echo "ERROR: No group name specified"
        exit 1
    fi
    if [ -z "${REP}" ]; then
        usage
        echo "ERROR: No replica set Id specified"
        exit 1
    fi
    if [ -z "${REPS}" ]; then
        usage
        echo "ERROR: No replica set specified"
        exit 1
    fi
    if [ -z "${PROTO}" ]; then
        usage
        echo "ERROR: No protocol uri specified"
        exit 1
    fi
    if [ -z "${T_PORT}" ]; then
        usage
        echo "ERROR: No tport specified"
        exit 1
    fi
}

function run()
{
    checkEnvironment
    checkStartFlags

    APP_ARGS="-e ${ENDPOINT} -g ${GROUP} -r ${REPS} -p ${T_PORT}"

    if [ -z "${QUERY_STRING}" ]; then
        APP_ARGS="${APP_ARGS} -u ${PROTO}://localhost:${PORT}"
    else
        APP_ARGS="${APP_ARGS} -u ${PROTO}://localhost:${PORT}/?${QUERY_STRING}"
    fi

    JAVA_OPTS=" ${JAVA_OPTS} -Djava.net.preferIPv4Stack=true -Dorg.slf4j.simpleLogger.defaultLogLevel=${LOG_LEVEL}"
    JAVA_OPTS="-server -Xmx1g -Xms1g ${JAVA_OPTS}"
    JCMD="${JAVA_OPTS} ${JMX} -jar ${KROTO_HOME}/${APP_JAR} ${APP_ARGS}"
    echo ${JCMD}
    java ${JCMD}
}

run