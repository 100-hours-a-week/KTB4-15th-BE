package com.ktb.lookddak.domain.fitting.entity;

import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.product.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class FittingCandidateTest {

    @Test
    @DisplayName("회원과 상품으로 피팅 후보를 생성한다")
    void createFittingCandidate() {
        Member member = Member.create("member@lookddak.com", "encoded");
        ReflectionTestUtils.setField(member, "id", 1L);
        Product product = Product.create(49_000);

        FittingCandidate candidate = FittingCandidate.create(member, product);

        assertThat(candidate.getMember()).isSameAs(member);
        assertThat(candidate.getProduct()).isSameAs(product);
        assertThat(candidate.isOwnedBy(1L)).isTrue();
        assertThat(candidate.isOwnedBy(2L)).isFalse();
    }
}
