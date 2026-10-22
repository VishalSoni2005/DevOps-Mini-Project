package com.supplychain.service;

import com.supplychain.exception.ResourceNotFoundException;
import com.supplychain.exception.SupplyChainException;
import com.supplychain.model.Product;
import com.supplychain.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service handling all product and inventory operations.
 * Extensive logging is included for ELK stack monitoring.
 */
@Service
@Transactional
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getAllProducts() {
        logger.info("INVENTORY_EVENT: action=LIST_ALL_PRODUCTS");
        List<Product> products = productRepository.findAll();
        logger.info("INVENTORY_EVENT: action=LIST_ALL_PRODUCTS, count={}", products.size());
        return products;
    }

    public Product getProductById(Long id) {
        logger.info("INVENTORY_EVENT: action=GET_PRODUCT, productId={}", id);
        return productRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("INVENTORY_ERROR: action=GET_PRODUCT, productId={}, error=NOT_FOUND", id);
                    return new ResourceNotFoundException("Product", "id", id);
                });
    }

    public Product getProductBySku(String sku) {
        logger.info("INVENTORY_EVENT: action=GET_PRODUCT_BY_SKU, sku={}", sku);
        return productRepository.findBySku(sku)
                .orElseThrow(() -> {
                    logger.error("INVENTORY_ERROR: action=GET_PRODUCT_BY_SKU, sku={}, error=NOT_FOUND", sku);
                    return new ResourceNotFoundException("Product", "sku", sku);
                });
    }

    public Product createProduct(Product product) {
        logger.info("INVENTORY_EVENT: action=CREATE_PRODUCT, sku={}, name={}", product.getSku(), product.getName());

        // Check for duplicate SKU
        productRepository.findBySku(product.getSku()).ifPresent(existing -> {
            logger.error("INVENTORY_ERROR: action=CREATE_PRODUCT, sku={}, error=DUPLICATE_SKU", product.getSku());
            throw new SupplyChainException(
                    "Product with SKU '" + product.getSku() + "' already exists", "DUPLICATE_SKU");
        });

        Product saved = productRepository.save(product);
        logger.info("INVENTORY_EVENT: action=PRODUCT_CREATED, productId={}, sku={}", saved.getId(), saved.getSku());
        return saved;
    }

    public Product updateProduct(Long id, Product productDetails) {
        logger.info("INVENTORY_EVENT: action=UPDATE_PRODUCT, productId={}", id);

        Product product = getProductById(id);
        product.setName(productDetails.getName());
        product.setSku(productDetails.getSku());
        product.setDescription(productDetails.getDescription());
        product.setPrice(productDetails.getPrice());
        product.setQuantityInStock(productDetails.getQuantityInStock());
        product.setReorderLevel(productDetails.getReorderLevel());
        product.setSupplier(productDetails.getSupplier());
        product.setCategory(productDetails.getCategory());
        product.setStatus(productDetails.getStatus());

        Product updated = productRepository.save(product);
        logger.info("INVENTORY_EVENT: action=PRODUCT_UPDATED, productId={}, sku={}", updated.getId(), updated.getSku());

        // Check stock levels
        checkStockLevel(updated);

        return updated;
    }

    public void deleteProduct(Long id) {
        logger.info("INVENTORY_EVENT: action=DELETE_PRODUCT, productId={}", id);
        Product product = getProductById(id);
        productRepository.delete(product);
        logger.warn("INVENTORY_EVENT: action=PRODUCT_DELETED, productId={}, sku={}", id, product.getSku());
    }

    public List<Product> getLowStockProducts() {
        logger.info("INVENTORY_EVENT: action=CHECK_LOW_STOCK");
        List<Product> lowStock = productRepository.findByStatus(Product.ProductStatus.LOW_STOCK);
        if (!lowStock.isEmpty()) {
            logger.warn("INVENTORY_ALERT: action=LOW_STOCK_DETECTED, count={}, products={}",
                    lowStock.size(),
                    lowStock.stream().map(Product::getSku).toList());
        }
        return lowStock;
    }

    /**
     * Simulates a stock level check — triggers warnings/errors for ELK monitoring.
     */
    private void checkStockLevel(Product product) {
        if (product.getQuantityInStock() != null && product.getReorderLevel() != null) {
            if (product.getQuantityInStock() <= 0) {
                product.setStatus(Product.ProductStatus.OUT_OF_STOCK);
                productRepository.save(product);
                logger.error("INVENTORY_CRITICAL: action=OUT_OF_STOCK, sku={}, name={}",
                        product.getSku(), product.getName());
            } else if (product.getQuantityInStock() <= product.getReorderLevel()) {
                product.setStatus(Product.ProductStatus.LOW_STOCK);
                productRepository.save(product);
                logger.warn("INVENTORY_ALERT: action=LOW_STOCK, sku={}, current={}, reorderLevel={}",
                        product.getSku(), product.getQuantityInStock(), product.getReorderLevel());
            }
        }
    }
}
