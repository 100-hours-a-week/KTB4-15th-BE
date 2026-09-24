package com.ktb.lookddak.domain.fitting.repository;

import com.ktb.lookddak.domain.fitting.entity.FittingCandidate;
import com.ktb.lookddak.domain.product.entity.ProductItemType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FittingCandidateRepository
        extends JpaRepository<FittingCandidate, Long> {

    boolean existsByMemberIdAndProductId(Long memberId, Long productId);

    long countByMemberId(Long memberId);

    @Query("""
            select candidate.product.id
            from FittingCandidate candidate
            where candidate.member.id = :memberId
              and candidate.product.id in :productIds
            """)
    Set<Long> findProductIdsByMemberIdAndProductIdIn(
            @Param("memberId") Long memberId,
            @Param("productIds") Collection<Long> productIds
    );

    @Query("""
            select candidate
            from FittingCandidate candidate
            join fetch candidate.product product
            where candidate.member.id = :memberId
            order by candidate.id desc
            """)
    List<FittingCandidate> findFirstPage(
            @Param("memberId") Long memberId,
            Pageable pageable
    );

    @Query("""
            select candidate
            from FittingCandidate candidate
            join fetch candidate.product product
            where candidate.member.id = :memberId
              and candidate.id < :cursor
            order by candidate.id desc
            """)
    List<FittingCandidate> findNextPage(
            @Param("memberId") Long memberId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    @Query("""
            select candidate
            from FittingCandidate candidate
            join fetch candidate.product product
            where candidate.member.id = :memberId
              and product.itemType = :itemType
            order by candidate.id desc
            """)
    List<FittingCandidate> findFirstPageByItemType(
            @Param("memberId") Long memberId,
            @Param("itemType") ProductItemType itemType,
            Pageable pageable
    );

    @Query("""
            select candidate
            from FittingCandidate candidate
            join fetch candidate.product product
            where candidate.member.id = :memberId
              and candidate.id < :cursor
              and product.itemType = :itemType
            order by candidate.id desc
            """)
    List<FittingCandidate> findNextPageByItemType(
            @Param("memberId") Long memberId,
            @Param("cursor") Long cursor,
            @Param("itemType") ProductItemType itemType,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select fc from FittingCandidate fc where fc.id = :candidateId")
    Optional<FittingCandidate> findByIdForUpdate(
            @Param("candidateId") Long candidateId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select candidate
            from FittingCandidate candidate
            where candidate.id in :candidateIds
            order by candidate.id asc
            """)
    List<FittingCandidate> findAllByIdInForUpdate(
            @Param("candidateIds") Collection<Long> candidateIds
    );
}
