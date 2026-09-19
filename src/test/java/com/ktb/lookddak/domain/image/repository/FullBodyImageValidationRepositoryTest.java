package com.ktb.lookddak.domain.image.repository;

import com.ktb.lookddak.domain.image.entity.FullBodyImageValidation;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class FullBodyImageValidationRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private FullBodyImageValidationRepository validationRepository;

    @Test
    @DisplayName("전신사진 검증 결과를 저장하고 사용 후 삭제한다")
    void saveAndDeleteValidation() {
        Member member = memberRepository.saveAndFlush(
                Member.create("validation@lookddak.com", "encoded-password")
        );
        FullBodyImageValidation validation = validationRepository.saveAndFlush(
                FullBodyImageValidation.create(
                        member,
                        "full-body/validation/1/test.png"
                )
        );

        assertThat(validation.getId()).isNotNull();
        assertThat(validation.getCreatedAt()).isNotNull();
        assertThat(validation.isOwnedBy(member.getId())).isTrue();

        validationRepository.delete(validation);
        validationRepository.flush();

        assertThat(validationRepository.findById(validation.getId())).isEmpty();
    }
}
