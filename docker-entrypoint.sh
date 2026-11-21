#!/bin/sh
echo "The application will start ..."
set -f   # 未加引号的变量只按空白分词，不做通配符展开
exec java ${JAVA_OPTS} ${APP_OPTS} -XX:+AlwaysPreTouch -Djava.security.egd=file:/dev/./urandom -jar app.jar  "$@"
