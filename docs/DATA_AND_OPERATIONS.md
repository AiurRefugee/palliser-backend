# 数据、日志和排障

## 表

| 表 | 内容 |
|---|---|
| `catalog_category` | 顶层和子分类；Benches 为非活动。 |
| `catalog_product` | 351 个按 URL slug 去重的商品摘要。 |
| `catalog_route` | 397 条 URL 与商品、分类关系，Ottoman 可跨两个目录。 |
| `catalog_product_detail` | 15 份详情业务 JSON。 |
| `catalog_series` | 13 份系列、材料、模块及选项 JSON。 |
| `cms_page` | 首页 CMS HTML 与精选商品。 |
| `api_request_log` | API 请求方式、路径、状态、耗时、请求 ID 和时间。 |
| `api_error_log` | 未处理的服务异常类型、消息、堆栈、请求 ID 和时间。 |

首次启动自动从 `seed/catalog.json` 导入；之后不覆盖原数据。清空开发 SQLite 文件可重建样本库，MySQL 生产库不要这样操作。

## 落库请求日志

从响应头 `X-Request-Id` 查询：

```sql
SELECT request_id,method,path,status,duration_ms,error_code,created_at
FROM api_request_log WHERE request_id='填入响应头的值';

SELECT path,COUNT(*) AS errors
FROM api_request_log WHERE status>=500 GROUP BY path ORDER BY errors DESC;

SELECT e.created_at,e.exception_class,e.message,e.stack_trace
FROM api_error_log e WHERE e.request_id='填入响应头的值';
```

不记录请求体、Cookie、Authorization、完整查询字符串或客户端 IP。未处理的异常堆栈同时写数据库和应用标准日志，数据库请求日志与错误日志通过请求 ID 关联；一般的 400/404 仅在请求表记录状态。健康检查和 OPTIONS 不记录。数据库不可写时在应用日志报警，该次请求不能保证落库。堆栈可能含业务异常消息，须限制两张日志表的读取权限并按公司政策定期清理，例如保留 30 天。

## Redis 和搜索

首页、系列响应缓存 5 分钟，Key 前缀为 `palliser:v1:`；Redis 故障时回源数据库。修改内容后删除相关 Key 或等待过期。加入市场、语言或登录态时，应将这些维度放入缓存键。

以后可把 `catalog_product + catalog_route + catalog_category` 投影到 Elasticsearch，实现全文搜索、筛选和聚合；MySQL 仍是主数据源，`GET /shop/products` 接口不变。索引需要可靠的重建和同步机制。

## 常见故障

- SQLite 无法打开：在仓库根目录创建可写的 `data/`；确认 Maven 已下载 `sqlite-jdbc`。
- MySQL 连不上：检查 `DB_URL/DB_USER/DB_PASSWORD`、数据库账户权限及网络，不要打印密码。
- Redis 不可用：服务应回源数据库；检查 `REDIS_HOST/PORT`。
- 商品详情为空：看 `detailAvailable`；只有 15 款具有完整详情捕获。
- 请求日志表没有记录：看标准日志里的 `Request log database write failed`，检查数据库写权限。
