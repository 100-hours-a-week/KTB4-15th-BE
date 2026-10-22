package com.ktb.lookddak.domain.fitting;

import com.jayway.jsonpath.JsonPath;
import com.ktb.lookddak.domain.fitting.entity.FittingJob;
import com.ktb.lookddak.domain.fitting.entity.FittingResult;
import com.ktb.lookddak.domain.fitting.entity.FittingTempResult;
import com.ktb.lookddak.domain.fitting.repository.FittingJobRepository;
import com.ktb.lookddak.domain.fitting.repository.FittingResultRepository;
import com.ktb.lookddak.domain.fitting.repository.FittingTempResultRepository;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.repository.MemberRepository;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FittingResultIntegrationTest {

    private static final String EMAIL = "fitting-result@lookddak.com";
    private static final String PASSWORD = "Test1234!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private FittingJobRepository fittingJobRepository;

    @Autowired
    private FittingTempResultRepository fittingTempResultRepository;

    @Autowired
    private FittingResultRepository fittingResultRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private FittingJob fittingJob;

    @BeforeEach
    void setUp() {
        Member member = memberRepository.saveAndFlush(Member.create(
                EMAIL,
                passwordEncoder.encode(PASSWORD)
        ));

        fittingJob = fittingJobRepository.saveAndFlush(FittingJob.create(member));
        fittingJob.completeGeneration();
        fittingJobRepository.saveAndFlush(fittingJob);

        fittingTempResultRepository.saveAndFlush(FittingTempResult.create(
                fittingJob,
                "fittings/result-" + fittingJob.getId() + ".png",
                "AI 기본 코디명",
                "AI가 생성한 코디 설명입니다."
        ));
    }

    @Test
    @DisplayName("완료된 가상피팅 결과를 저장하면 201 Created를 반환한다")
    void createFittingResult() throws Exception {
        String accessToken = loginAndGetAccessToken();

        MvcResult result = mockMvc.perform(post("/api/v1/fitting-results")
                        .cookie(new Cookie("accessToken", accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fittingJobId": %d,
                                  "outfitName": "회사 데일리 니트"
                                }
                                """.formatted(fittingJob.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CREATED"))
                .andExpect(jsonPath("$.data.outfitName").value("회사 데일리 니트"))
                .andExpect(jsonPath("$.message")
                        .value("리소스가 성공적으로 생성되었습니다."))
                .andReturn();

        Long fittingResultId = ((Number) JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.data.fittingResultId"
        )).longValue();
        FittingResult fittingResult = fittingResultRepository
                .findById(fittingResultId)
                .orElseThrow();

        assertThat(fittingResult.getFittingJob().getId())
                .isEqualTo(fittingJob.getId());
        assertThat(fittingResult.getResultImageKey())
                .isEqualTo("fittings/result-" + fittingJob.getId() + ".png");
        assertThat(fittingResult.getAiComment())
                .isEqualTo("AI가 생성한 코디 설명입니다.");
    }

    @Test
    @DisplayName("공백 코디명으로 저장하면 400 Bad Request를 반환한다")
    void rejectBlankOutfitName() throws Exception {
        String accessToken = loginAndGetAccessToken();

        mockMvc.perform(post("/api/v1/fitting-results")
                        .cookie(new Cookie("accessToken", accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fittingJobId": %d,
                                  "outfitName": "   "
                                }
                                """.formatted(fittingJob.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_OUTFIT_NAME"));
    }

    private String loginAndGetAccessToken() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        return loginResult.getResponse().getHeaders(HttpHeaders.SET_COOKIE)
                .stream()
                .filter(cookie -> cookie.startsWith("accessToken="))
                .findFirst()
                .orElseThrow()
                .substring("accessToken=".length())
                .split(";", 2)[0];
    }
}
