package com.ktb.lookddak.domain.member.repository;

import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.entity.MemberProfile;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class MemberProfileRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberProfileRepository memberProfileRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("회원 프로필을 저장하고 회원 ID로 존재 여부를 확인한다")
    void saveMemberProfile() {
        Member member = saveMember("profile@lookddak.com");

        MemberProfile profile = memberProfileRepository.saveAndFlush(
                createProfile(member, "김민준")
        );

        assertThat(profile.getId()).isNotNull();
        assertThat(profile.getCreatedAt()).isNotNull();
        assertThat(profile.getUpdatedAt()).isNotNull();
        assertThat(memberProfileRepository.existsByMemberId(member.getId())).isTrue();
    }

    @Test
    @DisplayName("한 회원은 프로필을 하나만 저장할 수 있다")
    void rejectDuplicatedMemberProfile() {
        Member member = saveMember("duplicate-profile@lookddak.com");
        memberProfileRepository.saveAndFlush(createProfile(member, "김민준"));

        MemberProfile duplicatedProfile = createProfile(member, "John Doe");

        assertThatThrownBy(() ->
                memberProfileRepository.saveAndFlush(duplicatedProfile)
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("활성 회원의 프로필과 회원 정보를 함께 조회한다")
    void findActiveProfileWithMember() {
        Member member = saveMember("get-profile@lookddak.com");
        memberProfileRepository.saveAndFlush(createProfile(member, "김민준"));
        entityManager.clear();

        MemberProfile profile = memberProfileRepository
                .findActiveByMemberIdWithMember(member.getId())
                .orElseThrow();

        assertThat(profile.getName()).isEqualTo("김민준");
        assertThat(profile.getMember().getEmail())
                .isEqualTo("get-profile@lookddak.com");
        assertThat(Persistence.getPersistenceUtil().isLoaded(
                profile,
                "member"
        )).isTrue();
    }

    @Test
    @DisplayName("탈퇴한 회원의 프로필은 조회하지 않는다")
    void excludeDeletedMemberProfile() {
        Member member = saveMember("deleted-profile@lookddak.com");
        memberProfileRepository.saveAndFlush(createProfile(member, "김민준"));
        ReflectionTestUtils.setField(
                member,
                "deletedAt",
                LocalDateTime.now()
        );
        memberRepository.saveAndFlush(member);
        entityManager.clear();

        assertThat(memberProfileRepository.findActiveByMemberIdWithMember(
                member.getId()
        )).isEmpty();
    }

    private Member saveMember(String email) {
        return memberRepository.saveAndFlush(
                Member.create(email, "encoded-password")
        );
    }

    private MemberProfile createProfile(Member member, String name) {
        return MemberProfile.create(
                member,
                name,
                29,
                new BigDecimal("175.5"),
                new BigDecimal("70.3"),
                "full-body/validation/1/test.png"
        );
    }
}
