# https://hub.docker.com/_/eclipse-temurin/tags
# docker.m.daocloud.io/ 是由 DaoCloud 提供服务的国内镜像源，代理加速下载 OpenJRE
FROM docker.m.daocloud.io/eclipse-temurin:25-jre
COPY ./bootstrap/target/*-exec.jar ./app.jar
COPY docker-entrypoint.sh /
RUN chmod +x ./docker-entrypoint.sh && \
    ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo Asia/Shanghai > /etc/timezone
ENTRYPOINT ["./docker-entrypoint.sh"]
