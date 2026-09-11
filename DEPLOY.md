# 线上一键部署

## 首次部署

```bash
cp .env.example .env
vi .env
./scripts/deploy.sh
```

脚本只使用 `pnpm` 构建前端，后端 Maven 在临时 Docker 容器内执行；随后构建并启动 Compose 服务，等待 Nacos 后自动导入 `resources/config/*.yaml`。

## 常用操作

```bash
docker compose ps
docker compose logs -f --tail=200 gateway-server
docker compose restart gateway-server
docker compose stop                 # 停止，不删除容器和数据
docker compose start                # 恢复运行
docker compose down                 # 删除容器和网络，保留数据卷
docker compose down -v              # 删除数据卷，等同于删除数据库/对象存储数据，谨慎执行
```

## 本机开发环境

开发时，Docker **只**常驻 MySQL、Redis、RabbitMQ、MinIO、Nacos 与判题沙箱；Java 服务和 Vite 前端均由 IDEA / 本机前台启动，不构建也不启动应用容器，不使用 SSL 证书。开发 Compose 是独立的 `docker-compose.dev.yaml`，不继承线上 Compose。

```bash
cp .env.dev.example .env.dev
vi .env.dev
./scripts/start-dev-infra.sh
```

开发 Compose 使用项目既有的本机默认连接参数（MySQL `root/change-this-db-password`、RabbitMQ `admin/admin`），因此可直接使用原有 IDEA 启动配置启动各 Java 服务，无需额外的端口或凭据覆盖。

也可在不同终端分别前台启动后端（需要本机 Maven 3.9+）和前端：

```bash
./scripts/run-local-service.sh user-service
./scripts/run-local-service.sh content-service
./scripts/run-local-service.sh problem-service
./scripts/run-local-service.sh judge-server
./scripts/run-local-service.sh gateway-server
./scripts/run-local-frontend.sh
```

网关在本机开发时使用 `http://127.0.0.1:18000`：此主机禁止普通用户监听小于 `1024` 的端口，无法使用原先的 `800`。线上 Compose 已传入 `SERVER_PORT=80`，不受本机默认值影响。前端 Vite 地址以终端输出为准。开发基础设施直接映射项目默认端口：Nacos `8848`、沙箱 `5050`、MySQL `3306`、Redis `6379`、RabbitMQ `5672`、MinIO `9000`。基础设施使用命名卷并设为 `unless-stopped`，会跨重启保持；应用进程不会保持在后台。

停止或查看基础设施：

```bash
docker compose -p oj-dev -f docker-compose.dev.yaml --env-file .env.dev stop oj-mysql oj-redis oj-mq oj-minio oj-nacos oj-sandbox
docker compose -p oj-dev -f docker-compose.dev.yaml --env-file .env.dev logs -f --tail=200 oj-nacos
```

此前容器化 Maven 构建可能会把 `*/target` 生成为不可由本机用户覆盖的文件；已清理当前仓库内这些旧产物。以后只使用本机 Maven / IDEA 构建，若 IDEA 仍缓存旧状态，执行 Maven Reload 后 Rebuild Project。

生产环境必须修改 `.env` 中的密码，并在防火墙中限制 3306、6379、5672、8848、9848、9849、5050、8000 和 MinIO 控制台端口的公网访问；通常只开放 80/443。
