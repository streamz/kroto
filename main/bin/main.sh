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
START=
WORK=
EXT=
PID=
ENDPOINT=
LOG_OUT=
LOG_ERR=
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
        -group)
            GROUP="${VALUE}"
            ;;
        -replica)
            REP="${VALUE}"
            ;;
        -endpoint)
            ENDPOINT="${VALUE}"
            ;;
        -proto)
            PROTO="${VALUE}"
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
        echo "ERROR: No replica set specified"
        exit 1
    fi
    if [ -z "${PROTO}" ]; then
        usage
        echo "ERROR: No protocol uri specified"
        exit 1
    fi
}

function run()
{
    checkEnvironment
    checkStartFlags

    APP_ARGS="-e ${ENDPOINT} -u ${PROTO}://localhost:${PORT} -g ${GROUP} -r ${REP}"

    JAVA_OPTS=" ${JAVA_OPTS} -Djava.net.preferIPv4Stack=true"
    JAVA_OPTS="-server -Xmx1g -Xms1g ${JAVA_OPTS}"
    JCMD="${JAVA_OPTS} ${JMX} -jar ${KROTO_HOME}/${APP_JAR} ${APP_ARGS}"
    echo ${JCMD}
    java ${JCMD}
}

run