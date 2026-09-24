package com.palliser.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogFlowTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @Test void completeReadFlow() throws Exception {
        mvc.perform(get("/api/v1/categories/tree"))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].path").value("/shop"));
        mvc.perform(get("/api/v1/shop/products").param("categoryUid","Mjc=").param("size","6"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(60))
            .andExpect(jsonPath("$.items.length()").value(6));
        mvc.perform(get("/api/v1/shop/products"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(349));
        mvc.perform(get("/api/v1/products/by-path")
                .param("path","/shop/living-room/sectionals/apex-44008-19"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.sku").value("44008"))
            .andExpect(jsonPath("$.detailAvailable").value(true));
        mvc.perform(get("/api/v1/series/APEX"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.models.length()").value(21));
        mvc.perform(get("/api/v1/pages/home"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.title").exists());
        long logged=jdbc.queryForObject("SELECT COUNT(*) FROM api_request_log",Long.class);
        org.junit.jupiter.api.Assertions.assertTrue(logged>=6);
    }

    @Test @Transactional void unexpectedErrorIsLinkedToRequestLog() throws Exception {
        jdbc.update("UPDATE catalog_product_detail SET payload_json=? WHERE slug=?","{","apex-44008-19");
        String requestId=mvc.perform(get("/api/v1/products/apex-44008-19"))
            .andExpect(status().isInternalServerError())
            .andReturn().getResponse().getHeader("X-Request-Id");
        org.junit.jupiter.api.Assertions.assertNotNull(requestId);
        org.junit.jupiter.api.Assertions.assertEquals(1L,
            jdbc.queryForObject("SELECT COUNT(*) FROM api_error_log WHERE request_id=?",Long.class,requestId));
        org.junit.jupiter.api.Assertions.assertEquals(1L,
            jdbc.queryForObject("SELECT COUNT(*) FROM api_request_log WHERE request_id=? AND status=500",Long.class,requestId));
    }
}
