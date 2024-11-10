FROM ubuntu:latest
LABEL authors="anishan"
LABEL version="1.0"
LABEL description="Judge Server For OJ"
LABEL privileged=true


ENV DEBIAN_FRONTEND=noninteractive
ENV TZ=Asia/Shanghai


# 阿里镜像
# 更新源并安装所需的编译器和解释器
RUN sed -i s@/archive.ubuntu.com/@/mirrors.aliyun.com/@g /etc/apt/sources.list && \
    apt-get clean && \
    apt update && \
    apt install -y \
    gcc \
    g++ \
    python3 \
    openjdk-17-jdk \
    golang \
    curl && \
    apt-get clean



# 显示安装的版本信息
RUN gcc --version && \
    g++ --version && \
    python3 --version && \
    java -version && \
    go version

WORKDIR /opt

COPY target/judge-server-1.0.0-beta.jar /opt
ADD go-judge_1.8.5_linux_386.tar.gz /opt

EXPOSE 8082

CMD ["sh", "-c", "/opt/go-judge & java -jar  judge-server-1.0.0-beta.jar", \
    "--judge.url=http://localhost:5050 --server.port=8082", \
    "--spring.cloud.nacos.server-addr=${NACOS_URL} --oj.db.host=${DB_HOST} --spring.redis.host=${REDIS_HOST} --oj.redis.host=${REDIS_HOST} ", \
    "--spring.rabbitmq.host=${MQ_HOST}", \
    "--spring.rabbitmq.virtual-host=${MQ_V_HOST}",\
    "--spring.rabbitmq.username=${MQ_USERNAME}",\
    "--spring.rabbitmq.password=${MQ_PASSWORD}",\
    "--spring.rabbitmq.listener.concurrency=${MQ_CONCURRENCY} --spring.rabbitmq.listener.max-concurrency=${MQ_MAX_CONCURRENCY}"\
    ]

