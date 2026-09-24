package com.palliser.backend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CatalogService {
    private static final Logger log=LoggerFactory.getLogger(CatalogService.class);
    private final CategoryMapper categories;
    private final ProductMapper products;
    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;
    private final ObjectMapper json;

    public CatalogService(CategoryMapper categories, ProductMapper products, JdbcTemplate jdbc,
                          StringRedisTemplate redis,ObjectMapper json) {
        this.categories=categories;this.products=products;this.jdbc=jdbc;this.redis=redis;this.json=json;
    }

    public List<Map<String,Object>> categoryTree() {
        List<CatalogCategory> all=categories.selectList(null);
        Map<String,Map<String,Object>> nodes=new LinkedHashMap<>();
        for (CatalogCategory c:all) {
            Map<String,Object> node=new LinkedHashMap<>();
            node.put("uid",c.getUid());node.put("name",c.getName());node.put("path",c.getPath());
            node.put("active",c.getActive());node.put("children",new ArrayList<Map<String,Object>>());
            nodes.put(c.getUid(),node);
        }
        List<Map<String,Object>> roots=new ArrayList<>();
        for (CatalogCategory c:all) {
            Map<String,Object> parent=nodes.get(c.getParentUid());
            if (parent==null) roots.add(nodes.get(c.getUid()));
            else children(parent).add(nodes.get(c.getUid()));
        }
        return roots;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String,Object>> children(Map<String,Object> node) {
        return (List<Map<String,Object>>)node.get("children");
    }

    public Map<String,Object> shop(String categoryUid,String search,int page,int size) {
        if (page<1||size<1||size>48) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"page >= 1 and 1 <= size <= 48 required");
        if (categoryUid!=null && categories.selectById(categoryUid)==null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"category not found");
        String cte= categoryUid==null ? "" : "WITH RECURSIVE subtree AS (SELECT uid FROM catalog_category WHERE uid=? UNION ALL SELECT c.uid FROM catalog_category c JOIN subtree s ON c.parent_uid=s.uid) ";
        String from=" FROM catalog_product p JOIN catalog_route r ON r.product_slug=p.slug JOIN catalog_category c ON c.uid=r.category_uid WHERE c.active=1";
        if (categoryUid!=null) from+=" AND r.category_uid IN (SELECT uid FROM subtree)";
        boolean searching=search!=null&&!search.isBlank();
        if (searching) from+=" AND LOWER(p.name) LIKE ?";
        List<Object> args=new ArrayList<>();
        if (categoryUid!=null) args.add(categoryUid);
        if (searching) args.add("%"+search.toLowerCase().trim()+"%");
        long total=jdbc.queryForObject(cte+"SELECT COUNT(DISTINCT p.slug)"+from,Long.class,args.toArray());
        String sql=cte+"SELECT p.slug,p.name,p.sku,p.small_image,p.detail_available,p.series_id,MIN(r.path) AS path"+from+
            " GROUP BY p.slug,p.name,p.sku,p.small_image,p.detail_available,p.series_id ORDER BY p.name,p.slug LIMIT ? OFFSET ?";
        List<Object> paging=new ArrayList<>(args);paging.add(size);paging.add((page-1)*size);
        List<Map<String,Object>> items=jdbc.query(sql,(rs,i)->{
            Map<String,Object> row=new LinkedHashMap<>();
            row.put("slug",rs.getString("slug"));row.put("name",rs.getString("name"));
            row.put("sku",rs.getString("sku"));row.put("smallImage",rs.getString("small_image"));
            row.put("detailAvailable",rs.getBoolean("detail_available"));
            row.put("seriesId",rs.getString("series_id"));row.put("path",rs.getString("path"));
            return row;
        },paging.toArray());
        return Map.of("items",items,"total",total,"page",page,"size",size,"totalPages",(total+size-1)/size);
    }

    public Map<String,Object> product(String slug) {
        CatalogProduct product=products.selectById(slug);
        if (product==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"product not found");
        List<String> routes=jdbc.query("SELECT path FROM catalog_route WHERE product_slug=? ORDER BY path",
            (rs,i)->rs.getString(1),slug);
        List<String> categoryUids=jdbc.query("SELECT DISTINCT category_uid FROM catalog_route WHERE product_slug=? ORDER BY category_uid",
            (rs,i)->rs.getString(1),slug);
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("slug",product.getSlug());result.put("name",product.getName());
        result.put("sku",product.getSku());result.put("smallImage",product.getSmallImage());
        result.put("detailAvailable",product.getDetailAvailable());result.put("seriesId",product.getSeriesId());
        result.put("routes",routes);result.put("categoryUids",categoryUids);
        if (Boolean.TRUE.equals(product.getDetailAvailable())) {
            String raw=jdbc.queryForObject("SELECT payload_json FROM catalog_product_detail WHERE slug=?",String.class,slug);
            result.put("detail",parse(raw));
        } else result.put("detail",null);
        return result;
    }

    public Map<String,Object> byPath(String path) {
        String clean=path.split("\\?",2)[0].replaceAll("/+$","");
        if (!clean.startsWith("/shop/")||clean.length()>512)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"invalid product path");
        try {
            String slug=jdbc.queryForObject("SELECT product_slug FROM catalog_route WHERE path=?",String.class,clean);
            return product(slug);
        } catch (EmptyResultDataAccessException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"route not found");
        }
    }

    public JsonNode series(String id) {
        return cache("series:"+id,()->{
            try {return parse(jdbc.queryForObject("SELECT payload_json FROM catalog_series WHERE id=?",String.class,id));}
            catch (EmptyResultDataAccessException e) {throw new ResponseStatusException(HttpStatus.NOT_FOUND,"series not found");}
        });
    }

    public Map<String,Object> home() {
        return cache("home",()->{
            Map<String,Object> row=jdbc.queryForMap("SELECT title,content_html,featured_slugs_json FROM cms_page WHERE slug='home'");
            List<Map<String,Object>> featured=new ArrayList<>();
            for (JsonNode slug:parse(row.get("featured_slugs_json").toString())) {
                CatalogProduct p=products.selectById(slug.asText());
                if(p!=null) {
                    String path=jdbc.query("SELECT path FROM catalog_route WHERE product_slug=? ORDER BY path LIMIT 1",
                        rs->rs.next()?rs.getString(1):null,p.getSlug());
                    if(path!=null) {
                        Map<String,Object> item=new LinkedHashMap<>();
                        item.put("slug",p.getSlug());item.put("name",p.getName());
                        item.put("path",path);item.put("smallImage",p.getSmallImage());
                        featured.add(item);
                    }
                }
            }
            Map<String,Object> result=new LinkedHashMap<>();
            result.put("title",row.get("title"));result.put("contentHtml",row.get("content_html"));
            result.put("featured",featured);return result;
        });
    }

    private JsonNode parse(String raw) {
        try {return json.readTree(raw);} catch(JsonProcessingException e) {throw new IllegalStateException("stored JSON invalid",e);}
    }
    private <T> T cache(String key,Supplier<T> source) {
        try {
            String cached=redis.opsForValue().get("palliser:v1:"+key);
            if(cached!=null) {
                @SuppressWarnings("unchecked") T value=(T)(key.equals("home")?json.readValue(cached,Map.class):json.readTree(cached));
                return value;
            }
        } catch(Exception e) {log.debug("Redis read unavailable: {}",e.toString());}
        T value=source.get();
        try {redis.opsForValue().set("palliser:v1:"+key,json.writeValueAsString(value),java.time.Duration.ofMinutes(5));}
        catch(Exception e) {log.debug("Redis write unavailable: {}",e.toString());}
        return value;
    }
}
