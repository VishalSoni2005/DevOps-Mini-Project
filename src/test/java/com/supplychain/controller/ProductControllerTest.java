package com.supplychain.controller;

import com.supplychain.model.Product;
import com.supplychain.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Test
    @DisplayName("GET /api/products should return product list")
    void getAllProducts_ReturnsOkWithList() throws Exception {
        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setSku("SKU-001");
        product.setPrice(100.0);

        when(productService.getAllProducts()).thenReturn(Arrays.asList(product));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].sku").value("SKU-001"))
                .andExpect(jsonPath("$[0].name").value("Test Product"));
    }

    @Test
    @DisplayName("GET /api/products/{id} should return product")
    void getProductById_ReturnsProduct() throws Exception {
        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setSku("SKU-001");
        product.setPrice(100.0);

        when(productService.getProductById(1L)).thenReturn(product);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-001"));
    }

    @Test
    @DisplayName("GET /api/products/low-stock should return low stock products")
    void getLowStockProducts_ReturnsLowStockList() throws Exception {
        Product product = new Product();
        product.setId(1L);
        product.setName("Low Stock Item");
        product.setSku("SKU-LOW-001");
        product.setStatus(Product.ProductStatus.LOW_STOCK);

        when(productService.getLowStockProducts()).thenReturn(Arrays.asList(product));

        mockMvc.perform(get("/api/products/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("LOW_STOCK"));
    }
}
