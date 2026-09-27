package com.ktb.lookddak.domain.fitting;

import com.jayway.jsonpath.JsonPath;
import com.ktb.lookddak.domain.fitting.entity.FittingCandidate;
import com.ktb.lookddak.domain.fitting.repository.FittingCandidateRepository;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.repository.MemberRepository;
import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.entity.ProductItemType;
import com.ktb.lookddak.domain.product.repository.ProductRepository;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FittingCandidateIntegrationTest {

    private static final String EMAIL = "fitting-integration@lookddak.com";
    private static final String PASSWORD = "Test1234!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private FittingCandidateRepository fittingCandidateRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Product product;
    private Member member;

    @BeforeEach
    void setUp() {
        member = memberRepository.saveAndFlush(Member.create(
                EMAIL,
                passwordEncoder.encode(PASSWORD)
        ));
        product = productRepository.saveAndFlush(Product.create(
                "0000001",
                "에센셜 램스울 크루넥",
                "https://image.lookddak.com/products/1.jpg",
                49_000,
                "네이비",
                ProductItemType.TOP,
                "https://shop.lookddak.com/products/1"
        ));
    }

    @Test
    @DisplayName("인증된 회원이 피팅 후보를 추가하고 하드 삭제한다")
    void createAndDeleteCandidate() throws Exception {
        String accessToken = loginAndGetAccessToken();

        MvcResult createResult = mockMvc.perform(
                        post("/api/v1/fitting-candidates")
                                .cookie(new Cookie("accessToken", accessToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"productId": %d}
                                        """.formatted(product.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CREATED"))
                .andExpect(jsonPath("$.data.productId").value(product.getId()))
                .andReturn();

        Long candidateId = ((Number) JsonPath.read(
                createResult.getResponse().getContentAsString(),
                "$.data.fittingCandidateId"
        )).longValue();

        mockMvc.perform(post("/api/v1/fitting-candidates")
                        .cookie(new Cookie("accessToken", accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": %d}
                                """.formatted(product.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("FITTING_CANDIDATE_ALREADY_EXISTS"));

        mockMvc.perform(delete(
                                "/api/v1/fitting-candidates/{candidateId}",
                                candidateId
                        )
                        .cookie(new Cookie("accessToken", accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data").isEmpty());

        assertThat(fittingCandidateRepository.findById(candidateId)).isEmpty();
        assertThat(productRepository.findById(product.getId())).isPresent();
    }

    @Test
    @DisplayName("인증된 회원이 피팅 후보 여러 개를 한 번에 하드 삭제한다")
    void bulkDeleteCandidates() throws Exception {
        Product secondProduct = productRepository.saveAndFlush(Product.create(
                "0000002",
                "와이드 데님 팬츠",
                "https://image.lookddak.com/products/2.jpg",
                59_000,
                "인디고",
                ProductItemType.BOTTOM,
                "https://shop.lookddak.com/products/2"
        ));
        FittingCandidate first = fittingCandidateRepository.save(
                FittingCandidate.create(member, product)
        );
        FittingCandidate second = fittingCandidateRepository.saveAndFlush(
                FittingCandidate.create(member, secondProduct)
        );
        String accessToken = loginAndGetAccessToken();

        mockMvc.perform(delete("/api/v1/fitting-candidates")
                        .cookie(new Cookie("accessToken", accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fittingCandidateIds": [%d, %d]
                                }
                                """.formatted(first.getId(), second.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.deletedCount").value(2));

        assertThat(fittingCandidateRepository.findAllById(
                List.of(first.getId(), second.getId())
        )).isEmpty();
        assertThat(productRepository.findAllById(
                List.of(product.getId(), secondProduct.getId())
        )).hasSize(2);
    }

    @Test
    @DisplayName("인증하지 않은 회원은 피팅 후보 API를 사용할 수 없다")
    void rejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(post("/api/v1/fitting-candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": %d}
                                """.formatted(product.getId())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
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
