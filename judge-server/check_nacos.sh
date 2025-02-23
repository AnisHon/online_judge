#!/bin/bash

while :
    do
        # 访问nacos注册中心，获取http状态码
        CODE=`curl -I -m 10 -o /dev/null -s -w %{http_code}  http://$NACOS_URL/nacos/index.html`
        # 判断状态码为200
        if [[ $CODE -eq 200 ]]; then
            # 输出绿色文字，并跳出循环
            echo -e "\033[42;34m nacos is ok \033[0m"
            break
        else
            # 暂停1秒
            sleep 1
        fi
    done

# while结束时，执行容器中的run.sh。
sh -c java -jar  judge-server-1.0.0-beta.jar \
  --oj.minio.url="${MINIO_URL}" \
  --judge.url="${JUDGE_URL}" \
  --server.port=8082 \
  --spring.cloud.nacos.server-addr="${NACOS_URL}" \
  --oj.db.host="${DB_HOST}" \
  --spring.redis.host="${REDIS_HOST}" \
  --oj.redis.host="${REDIS_HOST}" \
  --oj.mq.host="${MQ_HOST}" \
  --oj.mq.v-host="${MQ_V_HOST}" \
  --oj.mq.username="${MQ_USERNAME}" \
  --oj.mq.password="${MQ_PASSWORD}" \
  --spring.rabbitmq.listener.concurrency="${MQ_CONCURRENCY}" \
  --spring.rabbitmq.listener.max-concurrency="${MQ_MAX_CONCURRENCY}" \
  --oj.product=true
