FROM eclipse-temurin:17-jre-jammy
LABEL authors=anishan
LABEL version=1.0
LABEL description=ProblemService

ENV DEBIAN_FRONTEND=noninteractive
ENV TZ=Asia/Shanghai

WORKDIR /opt
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

COPY target/problem-service-1.0.0-beta.jar /opt

CMD ["sh", "-c",  "java -jar /opt/problem-service-1.0.0-beta.jar --spring.cloud.nacos.server-addr=${NACOS_URL} --oj.db.host=${DB_HOST} --spring.redis.host=${REDIS_HOST} --oj.redis.host=${REDIS_HOST}  --oj.mq.host=${MQ_HOST} --oj.mq.v-host=${MQ_V_HOST} --oj.mq.username=${MQ_USERNAME} --oj.mq.password=${MQ_PASSWORD} --oj.minio.url=${MINIO_URL} --oj.product=${OJ_PRODUCT:-false}"]
