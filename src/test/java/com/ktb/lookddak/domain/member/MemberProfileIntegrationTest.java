package com.ktb.lookddak.domain.member;

import com.ktb.lookddak.domain.image.entity.FullBodyImageValidation;
import com.ktb.lookddak.domain.image.repository.FullBodyImageValidationRepository;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.entity.MemberProfile;
import com.ktb.lookddak.domain.member.repository.MemberProfileRepository;
import com.ktb.lookddak.domain.member.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MemberProfileIntegrationTest {

    private static final String EMAIL = "profile-integration@lookddak.com";
    private static final String PASSWORD = "Test1234!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberProfileRepository memberProfileRepository;

    @Autowired
    private FullBodyImageValidationRepository validationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    private Member member;
    private FullBodyImageValidation validation;

    @BeforeEach
    void setUp() {
        member = memberRepository.saveAndFlush(Member.create(
                EMAIL,
                passwordEncoder.encode(PASSWORD)
        ));
        validation = validationRepository.saveAndFlush(
                FullBodyImageValidation.create(
                        member,
                        "full-body/validation/integration-test.png"
                )
        );
    }

    @Test
    @DisplayName("JWT 인증 회원이 검증된 전신사진으로 기본정보를 등록한다")
    void createProfileWithAuthentication() throws Exception {
        String accessToken = loginAndGetAccessToken();

        mockMvc.perform(post("/api/v1/members/me/profile")
                        .cookie(new Cookie("accessToken", accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "John Doe",
                                  "age": 29,
                                  "height": 175.5,
                                  "weight": 70.3,
                                  "fullBodyImageValidationId": %d,
                                  "priceAlertEnabled": true
                                }
                                """.formatted(validation.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CREATED"))
                .andExpect(jsonPath("$.data.profileId").isNumber())
                .andExpect(jsonPath("$.data.fullBodyImageKey")
                        .value("full-body/validation/integration-test.png"));

        entityManager.flush();
        entityManager.clear();

        Member savedMember = memberRepository.findById(member.getId()).orElseThrow();
        MemberProfile savedProfile = memberProfileRepository
                .findByMemberId(member.getId())
                .orElseThrow();

        assertThat(savedMember.isPriceAlertEnabled()).isTrue();
        assertThat(savedProfile.getName()).isEqualTo("John Doe");
        assertThat(savedProfile.getHeight()).isEqualByComparingTo("175.5");
        assertThat(savedProfile.getWeight()).isEqualByComparingTo("70.3");
        assertThat(savedProfile.getFullBodyImageKey())
                .isEqualTo("full-body/validation/integration-test.png");
        assertThat(validationRepository.findById(validation.getId())).isEmpty();
    }

    @Test
    @DisplayName("인증하지 않은 회원은 기본정보를 등록할 수 없다")
    void rejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(post("/api/v1/members/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "김민준",
                                  "age": 29,
                                  "height": 175.5,
                                  "weight": 70.3,
                                  "fullBodyImageValidationId": %d,
                                  "priceAlertEnabled": true
                                }
                                """.formatted(validation.getId())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private String loginAndGetAccessToken() throws Exception {
        List<String> setCookieHeaders = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeaders(HttpHeaders.SET_COOKIE);

        String prefix = "accessToken=";
        String accessTokenCookie = setCookieHeaders.stream()
                .filter(header -> header.startsWith(prefix))
                .findFirst()
                .orElseThrow();

        return accessTokenCookie.substring(
                prefix.length(),
                accessTokenCookie.indexOf(';')
        );
    }
}
