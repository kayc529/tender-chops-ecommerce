package com.kaycheung.product_service.dto;

import com.kaycheung.product_service.entity.ProductCategory;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductRequestDTO(@NotBlank String title,
                                String description,
                                String portionDescription,
                                @NotNull
                                @Digits(integer = 10, fraction = 2)
                                @DecimalMin("0.00")
                                BigDecimal price,
                                @NotNull ProductCategory productCategory) {
}
