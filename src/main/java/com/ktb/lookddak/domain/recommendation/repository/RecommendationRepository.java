package com.ktb.lookddak.domain.recommendation.repository;

import com.ktb.lookddak.domain.recommendation.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface RecommendationRepository
        extends JpaRepository<Recommendation, Long> {

    @Query("""
            select recommendation
            from Recommendation recommendation
            where recommendation.message.id in :messageIds
            """)
    List<Recommendation> findAllByMessageIdIn(
            @Param("messageIds") Collection<Long> messageIds
    );
}
