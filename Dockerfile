# https://hub.docker.com/_/eclipse-temurin/tags
FROM eclipse-temurin:25-jre
COPY ./bootstrap/target/*.jar ./app.jar
COPY docker-entrypoint.sh /
RUN chmod +x ./docker-entrypoint.sh && \
    ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo Asia/Shanghai > /etc/timezone
ENTRYPOINT ["./docker-entrypoint.sh"]
