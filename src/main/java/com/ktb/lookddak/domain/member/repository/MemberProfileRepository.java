package com.ktb.lookddak.domain.member.repository;

import com.ktb.lookddak.domain.member.entity.MemberProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberProfileRepository extends JpaRepository<MemberProfile, Long> {

    boolean existsByMemberId(Long memberId);

    Optional<MemberProfile> findByMemberId(Long memberId);
}
