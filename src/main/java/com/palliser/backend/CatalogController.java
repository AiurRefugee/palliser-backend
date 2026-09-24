package com.palliser.backend;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CatalogController {
    private final CatalogService catalog;
    public CatalogController(CatalogService catalog) {this.catalog=catalog;}

    @GetMapping("/categories/tree")
    public List<Map<String,Object>> categories() {return catalog.categoryTree();}

    @GetMapping("/shop/products")
    public Map<String,Object> shop(@RequestParam(required=false) String categoryUid,
                                   @RequestParam(required=false) String search,
                                   @RequestParam(defaultValue="1") int page,
                                   @RequestParam(defaultValue="24") int size) {
        return catalog.shop(categoryUid,search,page,size);
    }

    @GetMapping("/products/{slug}")
    public Map<String,Object> product(@PathVariable String slug) {return catalog.product(slug);}

    @GetMapping("/products/by-path")
    public Map<String,Object> productByPath(@RequestParam String path) {return catalog.byPath(path);}

    @GetMapping("/series/{id}")
    public JsonNode series(@PathVariable String id) {return catalog.series(id);}

    @GetMapping("/pages/home")
    public Map<String,Object> home() {return catalog.home();}
}
