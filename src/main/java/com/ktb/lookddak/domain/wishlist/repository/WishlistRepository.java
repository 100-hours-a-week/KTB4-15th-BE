package com.ktb.lookddak.domain.wishlist.repository;

import com.ktb.lookddak.domain.wishlist.entity.Wishlist;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    boolean existsByMemberIdAndProductId(Long memberId, Long productId);

    long countByMemberId(Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wishlist w where w.id = :wishlistId")
    Optional<Wishlist> findByIdForUpdate(@Param("wishlistId") Long wishlistId);
}
