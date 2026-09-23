package com.ktb.lookddak.domain.member.repository;

import com.ktb.lookddak.domain.member.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("회원을 저장하면 기본값과 생성·수정 일시가 설정된다")
    void saveMember() {
        Member member = Member.create("member@lookddak.com", "encoded-password");

        Member savedMember = memberRepository.saveAndFlush(member);

        assertThat(savedMember.getId()).isNotNull();
        assertThat(savedMember.isPriceAlertEnabled()).isFalse();
        assertThat(savedMember.getCreatedAt()).isNotNull();
        assertThat(savedMember.getUpdatedAt()).isNotNull();
        assertThat(savedMember.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("동일한 이메일은 중복 저장할 수 없다")
    void rejectDuplicatedEmail() {
        memberRepository.saveAndFlush(Member.create("duplicate@lookddak.com", "encoded-password"));

        Member duplicatedMember = Member.create("duplicate@lookddak.com", "another-password");

        assertThatThrownBy(() -> memberRepository.saveAndFlush(duplicatedMember))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
