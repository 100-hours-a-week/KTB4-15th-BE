package com.ktb.lookddak.domain.image.repository;

import com.ktb.lookddak.domain.image.entity.FullBodyImageValidation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FullBodyImageValidationRepository
        extends JpaRepository<FullBodyImageValidation, Long> {
}
