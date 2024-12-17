FROM ubuntu:latest
LABEL authors=anishan
LABEL version=1.0
LABEL description=UserService
LABEL privileged=true

ENV DEBIAN_FRONTEND=noninteractive
ENV TZ=Asia/Shanghai

RUN sed -i s@/archive.ubuntu.com/@/mirrors.aliyun.com/@g /etc/apt/sources.list && \
    apt-get update && apt-get upgrade -y && \
    apt-get install -y \
    openjdk-17-jdk \
    && apt-get clean

WORKDIR /opt

COPY target/gateway-server-1.0.0-beta.jar /opt

CMD ["sh", "-c", "java -jar /opt/gateway-server-1.0.0-beta.jar --spring.cloud.nacos.server-addr=${NACOS_URL} --server.port=${SERVER_PORT}"]