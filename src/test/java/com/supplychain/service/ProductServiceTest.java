package com.supplychain.service;

import com.supplychain.exception.ResourceNotFoundException;
import com.supplychain.exception.SupplyChainException;
import com.supplychain.model.Product;
import com.supplychain.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleProduct = new Product();
        sampleProduct.setId(1L);
        sampleProduct.setName("Test Motor");
        sampleProduct.setSku("SKU-TEST-001");
        sampleProduct.setPrice(500.0);
        sampleProduct.setQuantityInStock(20);
        sampleProduct.setReorderLevel(5);
        sampleProduct.setSupplier("TestSupplier");
        sampleProduct.setCategory("Electronics");
        sampleProduct.setStatus(Product.ProductStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should return all products")
    void getAllProducts_ReturnsProductList() {
        when(productRepository.findAll()).thenReturn(Arrays.asList(sampleProduct));

        List<Product> products = productService.getAllProducts();

        assertNotNull(products);
        assertEquals(1, products.size());
        verify(productRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return product by ID")
    void getProductById_ExistingId_ReturnsProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));

        Product found = productService.getProductById(1L);

        assertNotNull(found);
        assertEquals("SKU-TEST-001", found.getSku());
    }

    @Test
    @DisplayName("Should throw exception for non-existent product ID")
    void getProductById_NonExistentId_ThrowsException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.getProductById(999L));
    }

    @Test
    @DisplayName("Should create a new product")
    void createProduct_ValidProduct_ReturnsCreated() {
        when(productRepository.findBySku("SKU-TEST-001")).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        Product created = productService.createProduct(sampleProduct);

        assertNotNull(created);
        assertEquals("SKU-TEST-001", created.getSku());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw exception for duplicate SKU")
    void createProduct_DuplicateSku_ThrowsException() {
        when(productRepository.findBySku("SKU-TEST-001")).thenReturn(Optional.of(sampleProduct));

        assertThrows(SupplyChainException.class, () -> productService.createProduct(sampleProduct));
    }

    @Test
    @DisplayName("Should delete product by ID")
    void deleteProduct_ExistingId_DeletesSuccessfully() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        doNothing().when(productRepository).delete(any(Product.class));

        assertDoesNotThrow(() -> productService.deleteProduct(1L));
        verify(productRepository, times(1)).delete(any(Product.class));
    }

    @Test
    @DisplayName("Should detect low stock products")
    void getLowStockProducts_ReturnsLowStockList() {
        sampleProduct.setStatus(Product.ProductStatus.LOW_STOCK);
        when(productRepository.findByStatus(Product.ProductStatus.LOW_STOCK))
                .thenReturn(Arrays.asList(sampleProduct));

        List<Product> lowStock = productService.getLowStockProducts();

        assertEquals(1, lowStock.size());
        assertEquals(Product.ProductStatus.LOW_STOCK, lowStock.get(0).getStatus());
    }
}
