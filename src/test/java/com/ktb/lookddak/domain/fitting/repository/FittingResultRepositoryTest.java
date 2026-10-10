package com.ktb.lookddak.domain.fitting.repository;

import com.ktb.lookddak.domain.fitting.entity.FittingJob;
import com.ktb.lookddak.domain.fitting.entity.FittingResult;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(com.ktb.lookddak.global.config.JpaConfig.class)
class FittingResultRepositoryTest {

    @Autowired
    private FittingResultRepository fittingResultRepository;

    @Autowired
    private FittingJobRepository fittingJobRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member member;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.create(
                "fitting-result-repository@lookddak.com",
                "encoded-password"
        ));
    }

    @Test
    @DisplayName("삭제되지 않은 회원의 저장 결과를 최신순으로 조회한다")
    void findFirstPage() {
        FittingResult oldest = saveResult(member, "첫 번째 코디");
        FittingResult newest = saveResult(member, "두 번째 코디");
        FittingResult deleted = saveResult(member, "삭제된 코디");
        deleted.delete();
        fittingResultRepository.saveAndFlush(deleted);

        Member otherMember = memberRepository.save(Member.create(
                "other-fitting-result@lookddak.com",
                "encoded-password"
        ));
        saveResult(otherMember, "다른 회원 코디");

        List<FittingResult> results = fittingResultRepository.findFirstPage(
                member.getId(),
                PageRequest.of(0, 20)
        );

        assertThat(results).extracting(FittingResult::getId)
                .containsExactly(newest.getId(), oldest.getId());
    }

    @Test
    @DisplayName("커서보다 오래된 저장 결과만 최신순으로 조회한다")
    void findNextPage() {
        FittingResult oldest = saveResult(member, "첫 번째 코디");
        FittingResult middle = saveResult(member, "두 번째 코디");
        FittingResult newest = saveResult(member, "세 번째 코디");

        List<FittingResult> results = fittingResultRepository.findNextPage(
                member.getId(),
                middle.getId(),
                PageRequest.of(0, 20)
        );

        assertThat(results).extracting(FittingResult::getId)
                .containsExactly(oldest.getId());
        assertThat(newest.getId()).isGreaterThan(middle.getId());
    }

    @Test
    @DisplayName("삭제되지 않은 저장 결과만 집계한다")
    void countActiveByMemberId() {
        saveResult(member, "첫 번째 코디");
        FittingResult deleted = saveResult(member, "삭제된 코디");
        deleted.delete();
        fittingResultRepository.saveAndFlush(deleted);

        assertThat(fittingResultRepository.countActiveByMemberId(member.getId()))
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("상세 조회는 삭제되지 않은 저장 결과와 소유 회원을 함께 조회한다")
    void findActiveByIdWithMember() {
        FittingResult active = saveResult(member, "상세 조회 코디");
        FittingResult deleted = saveResult(member, "삭제된 상세 코디");
        deleted.delete();
        fittingResultRepository.saveAndFlush(deleted);

        FittingResult found = fittingResultRepository
                .findActiveByIdWithMember(active.getId())
                .orElseThrow();

        assertThat(found.getId()).isEqualTo(active.getId());
        assertThat(found.getFittingJob().getMember().getId())
                .isEqualTo(member.getId());
        assertThat(fittingResultRepository
                .findActiveByIdWithMember(deleted.getId())).isEmpty();
    }

    private FittingResult saveResult(Member owner, String outfitName) {
        FittingJob fittingJob = fittingJobRepository.save(FittingJob.create(owner));
        return fittingResultRepository.saveAndFlush(FittingResult.create(
                fittingJob,
                "fittings/" + outfitName + ".png",
                outfitName,
                "AI 코디 설명"
        ));
    }
}
