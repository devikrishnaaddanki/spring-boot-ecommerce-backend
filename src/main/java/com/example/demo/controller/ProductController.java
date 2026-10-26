package com.example.demo.controller;


import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ProductRequest;
import com.example.demo.dto.ProductResponse;
import com.example.demo.entity.Product;
import com.example.demo.service.ProductService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ProductResponse createProduct(
            @Valid @RequestBody ProductRequest request) {

        Product product = new Product();

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());

        Product savedProduct = productService.createProduct(product);

        return convertToResponse(savedProduct);
    }

    @GetMapping
    public List<ProductResponse> getAllProducts() {

        List<Product> products = productService.getAllProducts();

        return products.stream()
                .map(this::convertToResponse)
                .toList();
    }
    @GetMapping("/{id}")
    public ProductResponse getProductById(@PathVariable Long id) {

        Product product = productService.getProductById(id);

        return convertToResponse(product);
    }
    @PutMapping("/{id}")
    public ProductResponse updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {

        Product updatedProduct = productService.updateProduct(id, request);

        return convertToResponse(updatedProduct);
    }
    @DeleteMapping("/{id}")
    public void deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
    }
    @GetMapping("/paged")
    public Page<ProductResponse> getProducts(Pageable pageable) {

        Page<Product> products = productService.getProducts(pageable);

        return products.map(this::convertToResponse);
    }
    @GetMapping("/search")
    public List<ProductResponse> searchProducts(@RequestParam String name) {

        List<Product> products = productService.searchProducts(name);

        return products.stream()
                .map(this::convertToResponse)
                .toList();
    }
    @GetMapping("/filter")
    public List<ProductResponse> filterProductsByPrice(
            @RequestParam BigDecimal minPrice,
            @RequestParam BigDecimal maxPrice) {

        List<Product> products =
                productService.filterProductsByPrice(minPrice, maxPrice);

        return products.stream()
                .map(this::convertToResponse)
                .toList();
    }
    private ProductResponse convertToResponse(Product product) {

        ProductResponse response = new ProductResponse();

        response.setId(product.getId());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setStockQuantity(product.getStockQuantity());

        return response;
    }
}
