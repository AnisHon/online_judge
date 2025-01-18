FROM ubuntu
LABEL authors="anishan"
LABEL version="1.0"
LABEL privileged=true


ENV DEBIAN_FRONTEND=noninteractive
ENV TZ=Asia/Shanghai
ENV GIN_MODE=release

ADD go-judge_1.8.5_linux_386.tar.gz /opt
ADD run.sh /opt

# 阿里镜像
# 更新源并安装所需的编译器和解释器
RUN sed -i s@/archive.ubuntu.com/@/mirrors.aliyun.com/@g /etc/apt/sources.list  \
    && apt-get clean  \
    && apt update  \
    && apt install -y \
    gcc \
    g++ \
    python3 \
    openjdk-17-jdk \
    golang \
    curl  \
    s3fs  \
    && apt-get clean

# 显示安装的版本信息
RUN gcc --version \
    && g++ --version \
    && python3 --version \
    && java -version \
    && go version \
    && s3fs --version


RUN echo admin:change-this-object-storage-password > /etc/passwd-s3fs \
    && chmod 600 /etc/passwd-s3fs

RUN mkdir -p /mnt/minio/case


EXPOSE 5050

ENTRYPOINT ["bash", "/opt/run.sh"]
