package com.ktb.lookddak.domain.product.repository;

import com.ktb.lookddak.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
