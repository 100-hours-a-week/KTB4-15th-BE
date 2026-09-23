package com.ktb.lookddak.domain.wishlist.repository;

import com.ktb.lookddak.domain.wishlist.entity.Wishlist;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    boolean existsByMemberIdAndProductId(Long memberId, Long productId);

    long countByMemberId(Long memberId);

    @Query("""
            select wishlist.product.id
            from Wishlist wishlist
            where wishlist.member.id = :memberId
              and wishlist.product.id in :productIds
            """)
    Set<Long> findProductIdsByMemberIdAndProductIdIn(
            @Param("memberId") Long memberId,
            @Param("productIds") Collection<Long> productIds
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wishlist w where w.id = :wishlistId")
    Optional<Wishlist> findByIdForUpdate(@Param("wishlistId") Long wishlistId);
}
