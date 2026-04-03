package com.kaycheung.product_service.repository.specification;

import com.kaycheung.product_service.entity.Product;
import com.kaycheung.product_service.entity.ProductCategory;
import org.springframework.data.jpa.domain.Specification;

public class ProductSpecification {

    public static Specification<Product> hasTitle(String title) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%");
    }

    public static Specification<Product> hasCategory(ProductCategory category) {
        return (root, query, cb) ->
                cb.equal(root.get("productCategory"), category);
    }
}
