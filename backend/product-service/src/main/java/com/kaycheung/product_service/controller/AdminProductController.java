package com.kaycheung.product_service.controller;

import com.kaycheung.product_service.dto.ProductRequestDTO;
import com.kaycheung.product_service.dto.ProductResponseDTO;
import com.kaycheung.product_service.dto.UploadProductImageRequestDTO;
import com.kaycheung.product_service.dto.UploadProductImageResponseDTO;
import com.kaycheung.product_service.service.AdminProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/products")
public class AdminProductController {
    private final AdminProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponseDTO> createProduct(@Valid @RequestBody ProductRequestDTO request) {
        ProductResponseDTO response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{product_id}")
    public ProductResponseDTO updateProduct(@PathVariable("product_id") UUID productId, @Valid @RequestBody ProductRequestDTO request) {
        return productService.updateProduct(productId, request);
    }

    @DeleteMapping("/{product_id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable("product_id") UUID productId) {
        productService.deleteProduct(productId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{product_id}/image-upload-url")
    public UploadProductImageResponseDTO createImageUploadUrl(
            @PathVariable("product_id") UUID productId,
            @Valid @RequestBody UploadProductImageRequestDTO request) {
        return productService.createImageUploadUrl(productId, request);
    }
}
