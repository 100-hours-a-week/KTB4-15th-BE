package com.ktb.lookddak.domain.product.repository;

import com.ktb.lookddak.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllByProductCodeIn(Collection<String> productCodes);
}
