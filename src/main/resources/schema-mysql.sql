CREATE TABLE IF NOT EXISTS catalog_category (
  uid VARCHAR(80) PRIMARY KEY, name VARCHAR(255) NOT NULL,
  path VARCHAR(255) NOT NULL UNIQUE, parent_uid VARCHAR(80),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT fk_catalog_parent FOREIGN KEY(parent_uid) REFERENCES catalog_category(uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
CREATE TABLE IF NOT EXISTS catalog_product (
  slug VARCHAR(255) PRIMARY KEY, name VARCHAR(255) NOT NULL, sku VARCHAR(80),
  small_image VARCHAR(1024), detail_available BOOLEAN NOT NULL DEFAULT FALSE,
  series_id VARCHAR(80)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
CREATE TABLE IF NOT EXISTS catalog_route (
  path VARCHAR(512) PRIMARY KEY, product_slug VARCHAR(255) NOT NULL,
  category_uid VARCHAR(80) NOT NULL,
  KEY idx_catalog_route_category(category_uid,product_slug),
  CONSTRAINT fk_catalog_route_product FOREIGN KEY(product_slug) REFERENCES catalog_product(slug),
  CONSTRAINT fk_catalog_route_category FOREIGN KEY(category_uid) REFERENCES catalog_category(uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
CREATE TABLE IF NOT EXISTS catalog_product_detail (
  slug VARCHAR(255) PRIMARY KEY, payload_json JSON NOT NULL,
  CONSTRAINT fk_catalog_detail_product FOREIGN KEY(slug) REFERENCES catalog_product(slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
CREATE TABLE IF NOT EXISTS catalog_series (
  id VARCHAR(80) PRIMARY KEY, payload_json JSON NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
CREATE TABLE IF NOT EXISTS cms_page (
  slug VARCHAR(80) PRIMARY KEY, title VARCHAR(255), content_html LONGTEXT,
  featured_slugs_json JSON NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
CREATE TABLE IF NOT EXISTS api_request_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, request_id CHAR(36) NOT NULL,
  method VARCHAR(12) NOT NULL, path VARCHAR(512) NOT NULL, status SMALLINT NOT NULL,
  duration_ms BIGINT NOT NULL, error_code VARCHAR(80),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_api_request_log_created(created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
CREATE TABLE IF NOT EXISTS api_error_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, request_id CHAR(36),
  method VARCHAR(12) NOT NULL, path VARCHAR(512) NOT NULL,
  exception_class VARCHAR(255) NOT NULL, message TEXT,
  stack_trace MEDIUMTEXT NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_api_error_log_request(request_id),
  KEY idx_api_error_log_created(created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
