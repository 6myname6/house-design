FROM eclipse-temurin:17-jre
ENV TZ=Asia/Shanghai
# curl 供 compose healthcheck 探活 /actuator/health（temurin 基础镜像 Ubuntu jammy 不自带）
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT [ "java","-jar","app.jar" ]