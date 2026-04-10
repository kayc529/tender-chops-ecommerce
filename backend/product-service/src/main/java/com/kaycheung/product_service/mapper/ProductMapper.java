package com.kaycheung.product_service.mapper;

import com.kaycheung.product_service.dto.ProductListItemDTO;
import com.kaycheung.product_service.dto.ProductRequestDTO;
import com.kaycheung.product_service.dto.ProductResponseDTO;
import com.kaycheung.product_service.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Mapper(componentModel = "spring")
public abstract class ProductMapper {
    @Value("${app.media.base-url}")
    protected String mediaBaseUrl;

    //  Request DTO -> Entity
    @Mapping(target = "basePrice", expression = "java(convertToCents(dto.price()))")
    public abstract Product toEntity(ProductRequestDTO dto);

    //  Entity -> Response DTO
    @Mapping(target = "id", expression = "java(product.getId().toString())")
    @Mapping(target = "priceString", expression = "java(formatPrice(product.getBasePrice()))")
    @Mapping(target = "imageUrl", source = "imageKey", qualifiedByName = "mediaUrl")
    @Mapping(target = "thumbnailUrl", source = "thumbnailKey", qualifiedByName = "mediaUrl")
    @Mapping(target = "available", source = ".", qualifiedByName = "mapAvailable")
    @Mapping(target = "availableStock", source = ".", qualifiedByName = "mapAvailableStock")
    @Mapping(target = "availabilityStatus", source = "productStock.availabilityStatus")
    public abstract ProductResponseDTO toDetailDto(Product product);

    @Mapping(target = "id", expression = "java(product.getId().toString())")
    @Mapping(target = "priceString", expression = "java(formatPrice(product.getBasePrice()))")
    @Mapping(target = "thumbnailUrl", source = "thumbnailKey", qualifiedByName = "mediaUrl")
    @Mapping(target = "available", source = ".", qualifiedByName = "mapAvailable")
    @Mapping(target = "availableStock", source = ".", qualifiedByName = "mapAvailableStock")
    @Mapping(target = "availabilityStatus", source = "productStock.availabilityStatus")
    public abstract ProductListItemDTO toListDto(Product product);

    //  Update Product from RequestDTO without mapping the id
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "basePrice", expression = "java(convertToCents(dto.price()))")
    public abstract void updateProductFromRequestDTO(ProductRequestDTO dto, @MappingTarget Product product);

    //  List mapping
    public abstract List<ProductListItemDTO> toListItems(List<Product> products);

    @Named("mediaUrl")
    protected String buildMediaUrl(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String base = mediaBaseUrl.endsWith("/") ? mediaBaseUrl.substring(0, mediaBaseUrl.length() - 1) : mediaBaseUrl;
        return base + "/" + key;
    }

    //  Helper for formatting cents -> "xx.xx"
    protected String formatPrice(Long basePrice) {
        if (basePrice == null) {
            return null;
        }

        long cents = basePrice;
        long dollars = basePrice / 100;
        long remainder = Math.abs(cents % 100);

        return String.format("%d.%02d", dollars, remainder);
    }

    protected Long convertToCents(BigDecimal price) {
        if (price.scale() > 2) {
            throw new IllegalArgumentException("Price cannot have more than 2 decimal places");
        }

        return price
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.UNNECESSARY)
                .longValue();
    }

    @Named("mapAvailable")
    protected boolean mapAvailable(Product product) {
        return product.getProductStock() != null
                && product.getProductStock().getAvailableStock() != null
                && product.getProductStock().getAvailableStock() > 0;
    }

    @Named("mapAvailableStock")
    protected Long mapAvailableStock(Product product) {
        if (product.getProductStock() == null || product.getProductStock().getAvailableStock() == null) {
            return 0L;
        }
        return product.getProductStock().getAvailableStock().longValue();
    }

}
