package com.ktb.lookddak.domain.member.repository;

import com.ktb.lookddak.domain.member.entity.MemberProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberProfileRepository extends JpaRepository<MemberProfile, Long> {

    boolean existsByMemberId(Long memberId);

    Optional<MemberProfile> findByMemberId(Long memberId);

    @Query("""
            select profile
            from MemberProfile profile
            join fetch profile.member member
            where member.id = :memberId
              and member.deletedAt is null
            """)
    Optional<MemberProfile> findActiveByMemberIdWithMember(
            @Param("memberId") Long memberId
    );
}
