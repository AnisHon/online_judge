FROM eclipse-temurin:17-jre-jammy
LABEL authors=anishan
LABEL version=1.0
LABEL description=UserService
LABEL privileged=true

ENV DEBIAN_FRONTEND=noninteractive
ENV TZ=Asia/Shanghai

WORKDIR /opt

COPY target/user-service-1.0.0-beta.jar /opt
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

CMD ["sh", "-c", "java -jar /opt/user-service-1.0.0-beta.jar --spring.cloud.nacos.server-addr=${NACOS_URL} --oj.db.host=${DB_HOST} --spring.redis.host=${REDIS_HOST} --oj.redis.host=${REDIS_HOST} --oj.minio.url=${MINIO_URL} --oj.product=${OJ_PRODUCT:-false}"]
