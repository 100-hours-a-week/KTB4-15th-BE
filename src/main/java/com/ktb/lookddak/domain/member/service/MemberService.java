package com.ktb.lookddak.domain.member.service;

import com.ktb.lookddak.domain.member.dto.MemberMeResponse;
import com.ktb.lookddak.domain.member.repository.MemberProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberProfileRepository memberProfileRepository;

    public MemberMeResponse getMe(Long memberId) {
        boolean profileCompleted = memberProfileRepository
                .existsByMemberId(memberId);

        return new MemberMeResponse(profileCompleted);
    }
}
