package com.kaycheung.product_service.controller;

import com.kaycheung.product_service.dto.ProductPageResponse;
import com.kaycheung.product_service.dto.ProductRequestDTO;
import com.kaycheung.product_service.dto.ProductResponseDTO;
import com.kaycheung.product_service.entity.ProductCategory;
import com.kaycheung.product_service.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ProductPageResponse getAllProducts(
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "category", required = false) ProductCategory category,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return productService.getAllProducts(title, category, pageable);
    }

    @GetMapping("/{product_id}")
    public ProductResponseDTO getProduct(@PathVariable("product_id") UUID productId) {
        return productService.getProduct(productId);
    }
}
