package com.ktb.lookddak.domain.wishlist;

import com.jayway.jsonpath.JsonPath;
import com.ktb.lookddak.domain.member.entity.Member;
import com.ktb.lookddak.domain.member.repository.MemberRepository;
import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.repository.ProductRepository;
import com.ktb.lookddak.domain.wishlist.repository.WishlistRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WishlistIntegrationTest {

    private static final String EMAIL = "wishlist-integration@lookddak.com";
    private static final String PASSWORD = "Test1234!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Product product;

    @BeforeEach
    void setUp() {
        memberRepository.saveAndFlush(Member.create(
                EMAIL,
                passwordEncoder.encode(PASSWORD)
        ));
        product = productRepository.saveAndFlush(Product.create(49_000));
    }

    @Test
    @DisplayName("인증된 회원이 상품을 찜하고 하드 삭제한다")
    void createAndDeleteWishlist() throws Exception {
        String accessToken = loginAndGetAccessToken();

        MvcResult createResult = mockMvc.perform(post("/api/v1/wishlists")
                        .cookie(new Cookie("accessToken", accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": %d}
                                """.formatted(product.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CREATED"))
                .andExpect(jsonPath("$.data.productId").value(product.getId()))
                .andReturn();

        Long wishlistId = ((Number) JsonPath.read(
                createResult.getResponse().getContentAsString(),
                "$.data.wishlistId"
        )).longValue();
        assertThat(wishlistRepository.findById(wishlistId))
                .get()
                .extracting("wishedPrice")
                .isEqualTo(49_000);

        mockMvc.perform(post("/api/v1/wishlists")
                        .cookie(new Cookie("accessToken", accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": %d}
                                """.formatted(product.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WISHLIST_ALREADY_EXISTS"));

        mockMvc.perform(delete("/api/v1/wishlists/{wishlistId}", wishlistId)
                        .cookie(new Cookie("accessToken", accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data").isEmpty());

        assertThat(wishlistRepository.findById(wishlistId)).isEmpty();
    }

    @Test
    @DisplayName("인증하지 않은 회원은 찜 API를 사용할 수 없다")
    void rejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(post("/api/v1/wishlists")
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
