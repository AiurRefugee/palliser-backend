"""Export captured catalog data as portable MySQL 8 INSERT statements.

Run from the repository root: python3 scripts/export_mysql_seed.py
"""

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
source = json.loads((ROOT / "src/main/resources/seed/catalog.json").read_text(encoding="utf-8"))
target = ROOT / "sql/seed-mysql.sql"
target.parent.mkdir(exist_ok=True)


def literal(value):
    if value is None:
        return "NULL"
    if isinstance(value, bool):
        return "1" if value else "0"
    # Hex literals avoid SQL-mode-dependent escaping of HTML and JSON backslashes.
    return "CONVERT(0x" + str(value).encode("utf-8").hex() + " USING utf8mb4)"


def write_row(stream, table, fields, values):
    stream.write(
        f"INSERT INTO {table} ({', '.join(fields)}) VALUES "
        + "(" + ", ".join(map(literal, values)) + ");\n"
    )


with target.open("w", encoding="utf-8") as sql:
    sql.write("-- Run schema-mysql.sql first. Import into an empty MySQL 8 database.\n")
    sql.write("SET NAMES utf8mb4;\nSTART TRANSACTION;\n")
    for category in source["categories"]:
        write_row(sql, "catalog_category", ["uid", "name", "path", "parent_uid", "active"],
                  [category.get(k) for k in ("uid", "name", "path", "parentUid", "active")])
    for product in source["products"]:
        write_row(sql, "catalog_product", ["slug", "name", "sku", "small_image", "detail_available", "series_id"],
                  [product.get(k) for k in ("slug", "name", "sku", "smallImage", "detailAvailable", "seriesId")])
    category_by_path = {c["path"]: c["uid"] for c in source["categories"]}
    for product in source["products"]:
        for path in product["routes"]:
            parent = path.rsplit("/", 1)[0]
            write_row(sql, "catalog_route", ["path", "product_slug", "category_uid"],
                      [path, product["slug"], category_by_path[parent]])
    for slug, detail in source["productDetails"].items():
        write_row(sql, "catalog_product_detail", ["slug", "payload_json"],
                  [slug, json.dumps(detail, ensure_ascii=False, separators=(",", ":"))])
    for series_id, series in source["series"].items():
        write_row(sql, "catalog_series", ["id", "payload_json"],
                  [series_id, json.dumps(series, ensure_ascii=False, separators=(",", ":"))])
    home = source["home"]
    write_row(sql, "cms_page", ["slug", "title", "content_html", "featured_slugs_json"],
              ["home", home["title"], home["contentHtml"], json.dumps(home["featuredSlugs"], ensure_ascii=False)])
    sql.write("COMMIT;\n")
print(f"Wrote {target.relative_to(ROOT)}")
