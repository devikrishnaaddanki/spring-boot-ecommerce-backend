package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.example.demo.dto.ProductRequest;
import com.example.demo.entity.Product;
import com.example.demo.exception.ProductNotFoundException;
import com.example.demo.repository.ProductRepository;

class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getProductById_WhenProductExists_ReturnsProduct() {

        // Arrange
        Product product = new Product();
        product.setId(1L);
        product.setName("Laptop");
        product.setPrice(new BigDecimal("55000.00"));
        product.setStockQuantity(10);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        // Act
        Product result = productService.getProductById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Laptop", result.getName());

        verify(productRepository).findById(1L);
    }
    @Test
    void getProductById_WhenProductDoesNotExist_ThrowsException() {

        // Arrange
        when(productRepository.findById(99L))
                .thenReturn(Optional.empty());

        // Act and Assert
        ProductNotFoundException exception = assertThrows(
                ProductNotFoundException.class,
                () -> productService.getProductById(99L)
        );

        assertEquals(
                "Product not found with id: 99",
                exception.getMessage()
        );

        verify(productRepository).findById(99L);
    }
    @Test
    void createProduct_WhenProductIsValid_ReturnsSavedProduct() {

        // Arrange
        Product product = new Product();
        product.setName("Keyboard");
        product.setDescription("Mechanical keyboard");
        product.setPrice(new BigDecimal("2500.00"));
        product.setStockQuantity(20);

        Product savedProduct = new Product();
        savedProduct.setId(1L);
        savedProduct.setName("Keyboard");
        savedProduct.setDescription("Mechanical keyboard");
        savedProduct.setPrice(new BigDecimal("2500.00"));
        savedProduct.setStockQuantity(20);

        when(productRepository.save(product))
                .thenReturn(savedProduct);

        // Act
        Product result = productService.createProduct(product);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Keyboard", result.getName());

        verify(productRepository).save(product);
    }
    @Test
    void updateProduct_WhenProductExists_UpdatesAndReturnsProduct() {

        // Arrange
        Product existingProduct = new Product();
        existingProduct.setId(1L);
        existingProduct.setName("Old Laptop");
        existingProduct.setDescription("Old description");
        existingProduct.setPrice(new BigDecimal("45000.00"));
        existingProduct.setStockQuantity(5);

        ProductRequest request = new ProductRequest();
        request.setName("Gaming Laptop");
        request.setDescription("High-performance laptop");
        request.setPrice(new BigDecimal("75000.00"));
        request.setStockQuantity(10);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(existingProduct));

        when(productRepository.save(existingProduct))
                .thenReturn(existingProduct);

        // Act
        Product result = productService.updateProduct(1L, request);

        // Assert
        assertEquals("Gaming Laptop", result.getName());
        assertEquals("High-performance laptop", result.getDescription());
        assertEquals(new BigDecimal("75000.00"), result.getPrice());
        assertEquals(10, result.getStockQuantity());

        verify(productRepository).findById(1L);
        verify(productRepository).save(existingProduct);
    }
    @Test
    void updateProduct_WhenProductDoesNotExist_ThrowsException() {

        // Arrange
        ProductRequest request = new ProductRequest();
        request.setName("Gaming Laptop");
        request.setDescription("High-performance laptop");
        request.setPrice(new BigDecimal("75000.00"));
        request.setStockQuantity(10);

        when(productRepository.findById(99L))
                .thenReturn(Optional.empty());

        // Act and Assert
        ProductNotFoundException exception = assertThrows(
                ProductNotFoundException.class,
                () -> productService.updateProduct(99L, request)
        );

        assertEquals(
                "Product not found with id: 99",
                exception.getMessage()
        );

        // Verify that no product was saved
        verify(productRepository, never()).save(
                org.mockito.ArgumentMatchers.any(Product.class)
        );
    }
    @Test
    void deleteProduct_WhenProductExists_DeletesProduct() {

        // Arrange
        Product product = new Product();
        product.setId(1L);
        product.setName("Laptop");

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        // Act
        productService.deleteProduct(1L);

        // Assert
        verify(productRepository).findById(1L);
        verify(productRepository).delete(product);
    }
    @Test
    void deleteProduct_WhenProductDoesNotExist_ThrowsException() {

        // Arrange
        when(productRepository.findById(99L))
                .thenReturn(Optional.empty());

        // Act and Assert
        ProductNotFoundException exception = assertThrows(
                ProductNotFoundException.class,
                () -> productService.deleteProduct(99L)
        );

        assertEquals(
                "Product not found with id: 99",
                exception.getMessage()
        );

        // Verify that no deletion was attempted
        verify(productRepository, never()).delete(
                org.mockito.ArgumentMatchers.any(Product.class)
        );
    }
    @Test
    void getAllProducts_ReturnsProductList() {

        // Arrange
        Product product1 = new Product();
        product1.setId(1L);
        product1.setName("Laptop");

        Product product2 = new Product();
        product2.setId(2L);
        product2.setName("Keyboard");

        List<Product> products = List.of(product1, product2);

        when(productRepository.findAll())
                .thenReturn(products);

        // Act
        List<Product> result = productService.getAllProducts();

        // Assert
        assertEquals(2, result.size());
        assertEquals("Laptop", result.get(0).getName());
        assertEquals("Keyboard", result.get(1).getName());

        verify(productRepository).findAll();
    }
    @Test
    void searchProducts_ReturnsMatchingProducts() {

        // Arrange
        Product product1 = new Product();
        product1.setId(1L);
        product1.setName("Laptop");

        Product product2 = new Product();
        product2.setId(2L);
        product2.setName("Gaming Laptop");

        List<Product> products = List.of(product1, product2);

        when(productRepository.findByNameContainingIgnoreCase("laptop"))
                .thenReturn(products);

        // Act
        List<Product> result = productService.searchProducts("laptop");

        // Assert
        assertEquals(2, result.size());
        assertEquals("Laptop", result.get(0).getName());
        assertEquals("Gaming Laptop", result.get(1).getName());

        verify(productRepository)
                .findByNameContainingIgnoreCase("laptop");
    }
    @Test
    void filterProductsByPrice_ReturnsProductsWithinPriceRange() {

        // Arrange
        BigDecimal minPrice = new BigDecimal("1000.00");
        BigDecimal maxPrice = new BigDecimal("5000.00");

        Product product1 = new Product();
        product1.setId(1L);
        product1.setName("Keyboard");
        product1.setPrice(new BigDecimal("2500.00"));

        Product product2 = new Product();
        product2.setId(2L);
        product2.setName("Mouse");
        product2.setPrice(new BigDecimal("1500.00"));

        List<Product> products = List.of(product1, product2);

        when(productRepository.findByPriceBetween(minPrice, maxPrice))
                .thenReturn(products);

        // Act
        List<Product> result =
                productService.filterProductsByPrice(minPrice, maxPrice);

        // Assert
        assertEquals(2, result.size());
        assertEquals("Keyboard", result.get(0).getName());
        assertEquals("Mouse", result.get(1).getName());

        verify(productRepository)
                .findByPriceBetween(minPrice, maxPrice);
    }
    @Test
    void getProducts_ReturnsPagedProducts() {

        // Arrange
        Pageable pageable = PageRequest.of(0, 2);

        Product product1 = new Product();
        product1.setId(1L);
        product1.setName("Laptop");

        Product product2 = new Product();
        product2.setId(2L);
        product2.setName("Keyboard");

        Page<Product> productPage = new PageImpl<>(
                List.of(product1, product2),
                pageable,
                5
        );

        when(productRepository.findAll(pageable))
                .thenReturn(productPage);

        // Act
        Page<Product> result = productService.getProducts(pageable);

        // Assert
        assertEquals(2, result.getContent().size());
        assertEquals(5, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
        assertEquals("Laptop", result.getContent().get(0).getName());

        verify(productRepository).findAll(pageable);
    }
}
