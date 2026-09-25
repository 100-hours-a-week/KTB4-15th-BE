package com.ktb.lookddak.domain.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "product")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code", nullable = false, unique = true, length = 50)
    private String productCode;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "image_url", nullable = false, length = 2048)
    private String imageUrl;

    @Column(name = "current_price", nullable = false)
    private Integer currentPrice;

    @Column(nullable = false, length = 100)
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 20)
    private ProductItemType itemType;

    @Column(name = "purchase_url", nullable = false, length = 2048)
    private String purchaseUrl;

    private Product(
            String productCode,
            String name,
            String imageUrl,
            Integer currentPrice,
            String color,
            ProductItemType itemType,
            String purchaseUrl
    ) {
        this.productCode = productCode;
        this.name = name;
        this.imageUrl = imageUrl;
        this.currentPrice = currentPrice;
        this.color = color;
        this.itemType = itemType;
        this.purchaseUrl = purchaseUrl;
    }

    public static Product create(
            String productCode,
            String name,
            String imageUrl,
            Integer currentPrice,
            String color,
            ProductItemType itemType,
            String purchaseUrl
    ) {
        return new Product(
                productCode,
                name,
                imageUrl,
                currentPrice,
                color,
                itemType,
                purchaseUrl
        );
    }
}
