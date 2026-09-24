# Palliser 后端基础框架

Spring Boot + MySQL + MyBatis-Plus + Redis 的只读 REST 目录服务。开发环境默认使用 SQLite，无需安装 MySQL；Redis 暂时不可用时会回源数据库。

## 快速启动

先安装 JDK 17 和 Maven 3.9+，并保证能够下载 Maven 依赖。在仓库根目录执行：

```bash
mkdir -p data
mvn spring-boot:run
curl http://localhost:8080/actuator/health
curl 'http://localhost:8080/api/v1/shop/products?size=2'
```

首次启动创建 `data/palliser-dev.sqlite` 并自动导入 `src/main/resources/seed/catalog.json`。重启不会覆盖已导入数据。

```bash
python3 scripts/verify_seed.py
mvn test
```

## 切换 MySQL

先在 MySQL 8.x 创建空库，例如：

```sql
CREATE DATABASE palliser CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;
```

然后设置连接参数并启动：

```bash
export DB_URL='jdbc:mysql://localhost:3306/palliser?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC'
export DB_USER='palliser'
export DB_PASSWORD='实际密码'
export CORS_ORIGINS='http://localhost:3000'
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

此时会选用 `schema-mysql.sql`；Redis 地址可通过 `REDIS_HOST` 与 `REDIS_PORT` 设置。上线前应使用版本化数据库迁移，并设 `SEED_ENABLED=false` 关闭首次导入。不要导入之前交付的 22 表 SQL；那个文件是分析模型，当前服务使用另外 8 张表。

如需独立审查或人工导入 SQL，仓库还提供 [MySQL INSERT 数据](sql/seed-mysql.sql)。在空库先执行 `src/main/resources/schema-mysql.sql`，再执行该文件；应用启动时发现分类表已有数据，会跳过自动导入。修改 JSON 后可用 `python3 scripts/export_mysql_seed.py` 重新生成 INSERT 文件。请勿在已有数据的库里重复执行插入文件。

## 数据边界

现有资料覆盖 20 个分类节点（含非活动的 Benches）、351 个 URL 去重的商品标识、397 条分类路径、15 个完整 PDP 响应和 13 个系列响应。Shop 正常显示 349 款。对未采集 PDP 的商品，接口返回 `detailAvailable=false` 和 `detail=null`；名称可能由 URL 推导，图片可能为空。价格、库存、账号、购物车、订单和完整 3D 配置器尚无足够数据支撑。

首页 HTML 来自 Magento Page Builder，前端呈现前需按可信内容规则处理。详情与系列的原始 JSON 保留下来便于逐步结构化迁移。

继续阅读：[REST API](docs/API.md) · [数据库、日志与运维](docs/DATA_AND_OPERATIONS.md)。
