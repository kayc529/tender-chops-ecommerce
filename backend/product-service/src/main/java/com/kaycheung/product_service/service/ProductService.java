package com.kaycheung.product_service.service;

import com.kaycheung.product_service.dto.ProductListItemDTO;
import com.kaycheung.product_service.dto.ProductPageResponse;
import com.kaycheung.product_service.dto.ProductResponseDTO;
import com.kaycheung.product_service.entity.Product;
import com.kaycheung.product_service.entity.ProductCategory;
import com.kaycheung.product_service.exception.domain.ProductNotFoundException;
import com.kaycheung.product_service.mapper.ProductMapper;
import com.kaycheung.product_service.repository.ProductRepository;
import com.kaycheung.product_service.repository.specification.ProductSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final ProductCacheService productCacheService;


    public ProductPageResponse getAllProducts(String title, ProductCategory category, Pageable pageable) {
        boolean cacheable = (title == null || title.isBlank()) && pageable.getPageSize() == 20;

        if (cacheable) {
            Optional<ProductPageResponse> cachedList = productCacheService.getProductList(pageable.getPageNumber(), category);
            if (cachedList.isPresent()) {
                log.info("Cache hit for product list page={} category={}", pageable.getPageNumber(), category);
                return cachedList.get();
            }
        }

        List<Specification<Product>> specs = new ArrayList<>();

        if (title != null && !title.isBlank()) {
            specs.add(ProductSpecification.hasTitle(title));
        }

        if (category != null) {
            specs.add(ProductSpecification.hasCategory(category));
        }

        Specification<Product> combinedSpec = Specification.allOf(specs);
        Page<Product> products = productRepository.findAll(combinedSpec, pageable);

        List<ProductListItemDTO> dtos = productMapper.toListItems(products.getContent());
        ProductPageResponse pageResponse = new ProductPageResponse(
                dtos,
                products.getNumber(),
                products.getSize(),
                products.getTotalElements(),
                products.getTotalPages(),
                products.isFirst(),
                products.isLast(),
                products.hasNext(),
                products.hasPrevious()
        );

        if (cacheable) {
            log.info("Cache put for product list page={} category={}", pageable.getPageNumber(), category);
            productCacheService.putProductList(pageResponse, category);
        }

        return pageResponse;
    }

    public ProductResponseDTO getProduct(UUID productId) {
        Optional<ProductResponseDTO> cachedProduct = productCacheService.getProduct(productId);
        if (cachedProduct.isPresent()) {
            log.info("Cache hit for product {}", productId);
            return cachedProduct.get();
        }

        Product product = productRepository.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));
        ProductResponseDTO responseDTO = productMapper.toDetailDto(product);

        productCacheService.putProduct(responseDTO);

        return responseDTO;
    }
}
