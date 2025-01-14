FROM ubuntu
LABEL authors="anishan"
LABEL version="1.0"
LABEL privileged=true


ENV DEBIAN_FRONTEND=noninteractive
ENV TZ=Asia/Shanghai
ENV GIN_MODE=release

ADD go-judge_1.8.5_linux_386.tar.gz /opt


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
    && apt-get clean

# 显示安装的版本信息
RUN gcc --version \
    && g++ --version \
    && python3 --version \
    && java -version \
    && go version



EXPOSE 5050

ENTRYPOINT ["/opt/go-judge"]

