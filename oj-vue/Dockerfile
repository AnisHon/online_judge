FROM nginx

LABEL author=anishan



RUN rm /etc/nginx/conf.d/default.conf


ADD yantuocode.cn_nginx.tar.gz /etc/ssl/certs/
COPY default.conf /etc/nginx/conf.d/

VOLUME /etc/nginx/conf.d/
VOLUME /etc/ssl/certs/

RUN sed -i 's@deb.debian.org@repo.huaweicloud.com@g' /etc/apt/sources.list.d/debian.sources

COPY dist/ /usr/share/nginx/html/