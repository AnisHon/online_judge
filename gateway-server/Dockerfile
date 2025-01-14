FROM openjdk:17
LABEL authors=anishan
LABEL version=1.0
LABEL description=UserService
LABEL privileged=true

ENV DEBIAN_FRONTEND=noninteractive
ENV TZ=Asia/Shanghai

WORKDIR /opt

COPY target/gateway-server-1.0.0-beta.jar /opt

CMD ["sh", "-c", "java -jar /opt/gateway-server-1.0.0-beta.jar --spring.cloud.nacos.server-addr=${NACOS_URL} --server.port=${SERVER_PORT}"]