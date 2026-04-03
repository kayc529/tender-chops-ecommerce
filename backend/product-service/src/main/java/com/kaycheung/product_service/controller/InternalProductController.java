package com.kaycheung.product_service.controller;

import com.kaycheung.product_service.dto.InternalProductBasePriceRequestDTO;
import com.kaycheung.product_service.dto.InternalProductBasePriceResponseDTO;
import com.kaycheung.product_service.dto.UploadProductImageRequestDTO;
import com.kaycheung.product_service.dto.UploadProductImageResponseDTO;
import com.kaycheung.product_service.service.InternalProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/internal/products")
public class InternalProductController {
    private final InternalProductService productService;

    @PostMapping
    public InternalProductBasePriceResponseDTO getProducts(@Valid @RequestBody InternalProductBasePriceRequestDTO request) {
        return productService.getProductBasePrices(request);
    }
}
