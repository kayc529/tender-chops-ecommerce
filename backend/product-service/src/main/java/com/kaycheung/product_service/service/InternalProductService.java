package com.kaycheung.product_service.service;

import com.kaycheung.product_service.dto.InternalProductBasePriceDTO;
import com.kaycheung.product_service.dto.InternalProductBasePriceRequestDTO;
import com.kaycheung.product_service.dto.InternalProductBasePriceResponseDTO;
import com.kaycheung.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InternalProductService {

    private final ProductRepository productRepository;

    public InternalProductBasePriceResponseDTO getProductBasePrices(InternalProductBasePriceRequestDTO request) {
        List<InternalProductBasePriceDTO> products = productRepository.findByIdIn(request.productIds()).stream().map(p -> new InternalProductBasePriceDTO(p.getId(), p.getBasePrice())).toList();
        Set<UUID> foundProductIds = products.stream().map(InternalProductBasePriceDTO::productId).collect(Collectors.toSet());
        List<UUID> missingProducts = request.productIds().stream().filter(id -> !foundProductIds.contains(id)).toList();
        return new InternalProductBasePriceResponseDTO(products, missingProducts);
    }
}
