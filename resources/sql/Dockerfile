FROM mysql:8.0.27
LABEL author=anishan
ENV TZ=Asia/Shanghai
ENV MYSQL_ROOT_PASSWORD=change-this-default-password
ENV IS_INIT=FALSE

WORKDIR /opt

COPY run.sh /opt
COPY db_problem.sql /opt
COPY db_user.sql /opt
COPY db_content.sql /opt
COPY nacos.sql /opt

RUN chmod +x /opt/run.sh
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone


RUN echo "[mysqld]\n\
character-set-server=utf8mb4\n\
collation-server=utf8mb4_unicode_ci\n\
skip-character-set-client-handshake\n\
[client]\n\
default-character-set=utf8mb4\n\
[mysql]\n\
default-character-set=utf8mb4" > /etc/mysql/conf.d/custom.cnf

EXPOSE 3306
VOLUME /var/lib/mysql

CMD ["bash", "/opt/run.sh"]


