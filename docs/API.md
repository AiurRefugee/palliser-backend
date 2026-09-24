# REST API

本地基础地址：`http://localhost:8080/api/v1`。响应 JSON；`X-Request-Id` 可在数据库请求日志中查询。只读接口暂不要求登录。

| 页面 | 请求 | 参数与返回 |
|---|---|---|
| 导航 | `GET /categories/tree` | `uid,name,path,active,children` 分类树 |
| Shop | `GET /shop/products` | `categoryUid`、`search` 可选，`page` 从 1 开始，`size` 1–48；返回 `items,total,page,size,totalPages` |
| PDP | `GET /products/{slug}` | 商品摘要、`routes`、`categoryUids`、`detailAvailable`、`detail`、`seriesId` |
| PDP 路由 | `GET /products/by-path?path=...` | 以现有页面路径定位商品 |
| 配置数据 | `GET /series/{id}` | 例如 `APEX`，返回 `getSeriesData` 业务对象 |
| 首页 | `GET /pages/home` | `title,contentHtml,featured`；每个精选商品含 `slug,name,path,smallImage` |
| 服务状态 | `GET /actuator/health` | Spring Boot 健康状态 |

例子：

```bash
curl 'http://localhost:8080/api/v1/shop/products?categoryUid=Mjc%3D&size=6'
curl 'http://localhost:8080/api/v1/products/by-path?path=%2Fshop%2Fliving-room%2Fsectionals%2Fapex-44008-19'
curl 'http://localhost:8080/api/v1/series/APEX'
```

Sectionals (`Mjc=`) 应显示 `total=60`；Shop 全站正常列表显示 349，Bedroom 42，Home Accents 49。无效分页为 400，不存在的路径或实体为 404。商品 `detailAvailable=false` 表示仅有路径和分类，不能把 `detail=null` 当成抓取失败。

前端应独立设置 API base URL（例如 `NEXT_PUBLIC_API_BASE_URL`，具体以 `palliser-ui` 的现有变量为准）。图片和 3D 模型仍可沿用局域网资源 base URL，本后端不会重写它们。首页 `contentHtml` 是源站 CMS HTML，前端不要对不可信输入无条件使用 `dangerouslySetInnerHTML`。
