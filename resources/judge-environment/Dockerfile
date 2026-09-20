FROM ubuntu:24.04

LABEL org.opencontainers.image.title="延拓Code judge environment"
LABEL org.opencontainers.image.description="Pinned compiler/runtime environment and GoJudge for the online judge sandbox"

ARG DEBIAN_FRONTEND=noninteractive
ENV DEBIAN_FRONTEND=${DEBIAN_FRONTEND}
ENV TZ=Asia/Shanghai
ENV GIN_MODE=release
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH=${JAVA_HOME}/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

# The build context is resources/sandbox so the GoJudge archive is kept in one place.
ADD go-judge_1.8.5_linux_386.tar.gz /opt

# Debian/Ubuntu packages keep these JDK files as /etc symlinks. GoJudge's
# child mount namespace does not include that optional /etc path, so the files
# are made self-contained under JAVA_HOME before entering the sandbox.
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        ca-certificates \
        curl \
        gcc \
        g++ \
        golang \
        locales \
        mono-mcs \
        mono-runtime \
        nodejs \
        openjdk-17-jdk-headless \
        php-cli \
        pypy3 \
        python3 \
        ruby \
        rustc \
        s3fs \
        tzdata \
        util-linux \
    && sed -i '/^# *en_US.UTF-8 UTF-8/s/^# *//' /etc/locale.gen \
    && locale-gen en_US.UTF-8 \
    && update-locale LANG=en_US.UTF-8 LANGUAGE=en_US:en LC_ALL=en_US.UTF-8 \
    && ln -snf /usr/share/zoneinfo/${TZ} /etc/localtime \
    && echo ${TZ} > /etc/timezone \
    && for security_file in java.policy java.security nss.cfg; do \
        cp -L "${JAVA_HOME}/conf/security/${security_file}" "/tmp/${security_file}"; \
        rm -f "${JAVA_HOME}/conf/security/${security_file}"; \
        mv "/tmp/${security_file}" "${JAVA_HOME}/conf/security/${security_file}"; \
    done \
    && chmod 0755 /opt/go-judge \
    && sed -i '/# java 17 wants \/etc\/java-17-openjdk/,/readonly: true/d' /opt/mount.yaml \
    && test -r "${JAVA_HOME}/conf/security/java.security" \
    && java -version \
    && javac -version \
    && gcc --version \
    && g++ --version \
    && python3 --version \
    && pypy3 --version \
    && go version \
    && mcs --version \
    && php --version \
    && node --version \
    && ruby --version \
    && rustc --version \
    && s3fs --version \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /mnt/minio/case /tmp/cache \
    && chmod 1777 /tmp/cache

ENV LANG=en_US.UTF-8
ENV LANGUAGE=en_US:en
ENV LC_ALL=en_US.UTF-8

EXPOSE 5050
