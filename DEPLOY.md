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

## 开发环境部署

开发环境使用独立项目名、容器名、数据卷和端口，不会覆盖线上环境，也不会加载 SSL 证书：

```bash
cp .env.dev.example .env.dev
vi .env.dev
./scripts/deploy-dev.sh
```

默认访问地址为 `http://服务器IP:8080/`。开发环境端口：前端 8080、Nacos 18848、网关 18000、沙箱 15050、MySQL 13306、Redis 16379、RabbitMQ 5673、MinIO 19000。开发环境停止和查看日志：

```bash
docker compose -p oj-dev -f docker-compose.yaml -f docker-compose.dev.yaml --env-file .env.dev stop
docker compose -p oj-dev -f docker-compose.yaml -f docker-compose.dev.yaml --env-file .env.dev logs -f --tail=200 oj-vue
```

数据库、Redis、RabbitMQ、MinIO 和日志都放在 Compose 命名卷中，不依赖宿主机 `/opt` 目录。第一次从旧版部署迁移时，先备份旧的 `/opt/mysql/data`，确认数据迁移完成后再执行新 Compose。

生产环境必须修改 `.env` 中的密码，并在防火墙中限制 3306、6379、5672、8848、9848、9849、5050、8000 和 MinIO 控制台端口的公网访问；通常只开放 80/443。
