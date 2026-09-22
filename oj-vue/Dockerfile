FROM nginx:1.27-alpine

LABEL author=anishan



RUN rm /etc/nginx/conf.d/default.conf

COPY default.conf /etc/nginx/conf.d/

COPY dist/ /usr/share/nginx/html/
