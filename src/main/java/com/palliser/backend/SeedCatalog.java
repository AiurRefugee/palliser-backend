package com.palliser.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** First-run seed from captured public data. Never overwrites edited rows on restart. */
@Component
public class SeedCatalog implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final boolean enabled;
    public SeedCatalog(JdbcTemplate jdbc, ObjectMapper json, @Value("${app.seed-enabled:true}") boolean enabled) {
        this.jdbc=jdbc; this.json=json; this.enabled=enabled;
    }
    @Override @Transactional
    public void run(ApplicationArguments args) throws IOException {
        if (!enabled || jdbc.queryForObject("SELECT COUNT(*) FROM catalog_category", Integer.class)>0) return;
        JsonNode root;
        try (var input=new ClassPathResource("seed/catalog.json").getInputStream()) {
            root=json.readTree(input);
        }
        for (JsonNode c:root.path("categories")) {
            jdbc.update("INSERT INTO catalog_category(uid,name,path,parent_uid,active) VALUES (?,?,?,?,?)",
                str(c,"uid"),str(c,"name"),str(c,"path"),str(c,"parentUid"),c.path("active").asBoolean());
        }
        for (JsonNode p:root.path("products")) {
            jdbc.update("INSERT INTO catalog_product(slug,name,sku,small_image,detail_available,series_id) VALUES (?,?,?,?,?,?)",
                str(p,"slug"),str(p,"name"),str(p,"sku"),str(p,"smallImage"),p.path("detailAvailable").asBoolean(),str(p,"seriesId"));
        }
        for (JsonNode p:root.path("products")) {
            for (JsonNode route:p.path("routes")) {
                String path=route.asText();
                String parent=path.substring(0,path.lastIndexOf('/'));
                JsonNode category=findCategory(root.path("categories"),parent);
                if (category==null) throw new IllegalArgumentException("Unknown category for "+path);
                jdbc.update("INSERT INTO catalog_route(path,product_slug,category_uid) VALUES (?,?,?)",
                    path,str(p,"slug"),str(category,"uid"));
            }
        }
        root.path("productDetails").fields().forEachRemaining(e->jdbc.update(
            "INSERT INTO catalog_product_detail(slug,payload_json) VALUES (?,?)",e.getKey(),e.getValue().toString()));
        root.path("series").fields().forEachRemaining(e->jdbc.update(
            "INSERT INTO catalog_series(id,payload_json) VALUES (?,?)",e.getKey(),e.getValue().toString()));
        JsonNode home=root.path("home");
        jdbc.update("INSERT INTO cms_page(slug,title,content_html,featured_slugs_json) VALUES (?,?,?,?)",
            "home",str(home,"title"),str(home,"contentHtml"),home.path("featuredSlugs").toString());
    }
    private static JsonNode findCategory(JsonNode categories,String path) {
        for (JsonNode cat:categories) if (path.equals(str(cat,"path"))) return cat;
        return null;
    }
    private static String str(JsonNode x,String key) { return x.path(key).isMissingNode()||x.path(key).isNull()?null:x.path(key).asText(); }
}
