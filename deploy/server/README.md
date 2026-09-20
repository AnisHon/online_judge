# 服务器部署包

服务器只负责加载本地构建好的镜像、备份/迁移数据库并启动容器，不执行 Maven、pnpm 或 Docker build。

发布目录固定为 `/opt/online-judge`，现网 MySQL、Redis、RabbitMQ、MinIO 继续沿用原来的容器和 `/opt` 数据目录。执行 `RELEASE_TAG=<tag> ./deploy.sh` 会：

1. 从现有中间件容器提取凭据，并保留已经生成的三组 JWT 密钥；
2. 在 `backups/` 创建迁移前的 MySQL 全库压缩备份；
3. 重建 Nacos，清理旧容器可写层，同时保留 MySQL 中的 Nacos 配置；
4. 同步仓库内的 Nacos 配置并执行可重复运行的数据库迁移；
5. 切换沙箱、后端和前端镜像并做 HTTPS 存活检查。

数据目录和数据库备份不会被自动清理。镜像包加载完成后可删除，旧的无引用镜像应在服务验证通过后再清理。
