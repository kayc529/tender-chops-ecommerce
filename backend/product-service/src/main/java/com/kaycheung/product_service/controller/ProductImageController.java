package com.kaycheung.product_service.controller;

import com.kaycheung.product_service.dto.ProductImageUploadResultRequestDTO;
import com.kaycheung.product_service.service.ProductImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/internal/product-images")
public class ProductImageController {
    private final ProductImageService productImageService;

    @PostMapping("/result")
    public void confirmProductImageUploadResult(@RequestHeader("X-Internal-Token")String token, @Valid @RequestBody ProductImageUploadResultRequestDTO request){
        productImageService.confirmProductImageUploadResult(token, request);
    }
}
