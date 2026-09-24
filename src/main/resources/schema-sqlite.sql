CREATE TABLE IF NOT EXISTS catalog_category (
  uid TEXT PRIMARY KEY, name TEXT NOT NULL, path TEXT NOT NULL UNIQUE,
  parent_uid TEXT, active INTEGER NOT NULL DEFAULT 1
);
CREATE TABLE IF NOT EXISTS catalog_product (
  slug TEXT PRIMARY KEY, name TEXT NOT NULL, sku TEXT,
  small_image TEXT, detail_available INTEGER NOT NULL DEFAULT 0,
  series_id TEXT
);
CREATE TABLE IF NOT EXISTS catalog_route (
  path TEXT PRIMARY KEY, product_slug TEXT NOT NULL, category_uid TEXT NOT NULL,
  FOREIGN KEY(product_slug) REFERENCES catalog_product(slug),
  FOREIGN KEY(category_uid) REFERENCES catalog_category(uid)
);
CREATE INDEX IF NOT EXISTS idx_catalog_route_category ON catalog_route(category_uid,product_slug);
CREATE TABLE IF NOT EXISTS catalog_product_detail (
  slug TEXT PRIMARY KEY, payload_json TEXT NOT NULL,
  FOREIGN KEY(slug) REFERENCES catalog_product(slug)
);
CREATE TABLE IF NOT EXISTS catalog_series (
  id TEXT PRIMARY KEY, payload_json TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS cms_page (
  slug TEXT PRIMARY KEY, title TEXT, content_html TEXT,
  featured_slugs_json TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS api_request_log (
  id INTEGER PRIMARY KEY AUTOINCREMENT, request_id TEXT NOT NULL,
  method TEXT NOT NULL, path TEXT NOT NULL, status INTEGER NOT NULL,
  duration_ms INTEGER NOT NULL, error_code TEXT, created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_api_request_log_created ON api_request_log(created_at);
CREATE TABLE IF NOT EXISTS api_error_log (
  id INTEGER PRIMARY KEY AUTOINCREMENT, request_id TEXT,
  method TEXT NOT NULL, path TEXT NOT NULL,
  exception_class TEXT NOT NULL, message TEXT,
  stack_trace TEXT NOT NULL, created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_api_error_log_request ON api_error_log(request_id);
CREATE INDEX IF NOT EXISTS idx_api_error_log_created ON api_error_log(created_at);
