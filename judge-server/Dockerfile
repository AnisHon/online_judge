FROM openjdk:17
LABEL authors="anishan"
LABEL version="1.0"
LABEL description="Judge Server For OJ"
LABEL privileged=true


ENV DEBIAN_FRONTEND=noninteractive
ENV TZ=Asia/Shanghai

WORKDIR /opt

COPY target/judge-server-1.0.0-beta.jar /opt

EXPOSE 8082

CMD ["sh", "-c", "java -jar  judge-server-1.0.0-beta.jar --oj.minio.url=${MINIO_URL} --judge.url=${JUDGE_URL} --server.port=8082 --spring.cloud.nacos.server-addr=${NACOS_URL} --oj.db.host=${DB_HOST} --spring.redis.host=${REDIS_HOST} --oj.redis.host=${REDIS_HOST} --oj.mq.host=${MQ_HOST} --oj.mq.v-host=${MQ_V_HOST} --oj.mq.username=${MQ_USERNAME} --oj.mq.password=${MQ_PASSWORD} --spring.rabbitmq.listener.concurrency=${MQ_CONCURRENCY} --spring.rabbitmq.listener.max-concurrency=${MQ_MAX_CONCURRENCY} --oj.product=true"]

