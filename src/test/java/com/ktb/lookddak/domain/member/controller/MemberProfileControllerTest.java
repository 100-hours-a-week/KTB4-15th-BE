package com.ktb.lookddak.domain.member.controller;

import com.ktb.lookddak.domain.member.dto.MemberProfileCreateResponse;
import com.ktb.lookddak.domain.member.service.MemberProfileService;
import com.ktb.lookddak.global.exception.BusinessException;
import com.ktb.lookddak.global.exception.ErrorCode;
import com.ktb.lookddak.global.exception.GlobalExceptionHandler;
import com.ktb.lookddak.global.security.principal.MemberPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MemberProfileControllerTest {

    @Mock
    private MemberProfileService memberProfileService;

    @Mock
    private MemberPrincipal principal;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MemberProfileController controller =
                new MemberProfileController(memberProfileService);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        lenient().when(principal.getMemberId()).thenReturn(1L);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        List.of()
                )
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("인증된 회원이 기본정보를 등록하면 201 Created를 반환한다")
    void createProfile() throws Exception {
        given(memberProfileService.createProfile(any(), any()))
                .willReturn(new MemberProfileCreateResponse(
                        10L,
                        "full-body/validation/1/test.png"
                ));

        mockMvc.perform(post("/api/v1/members/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CREATED"))
                .andExpect(jsonPath("$.data.profileId").value(10))
                .andExpect(jsonPath("$.data.fullBodyImageKey")
                        .value("full-body/validation/1/test.png"))
                .andExpect(jsonPath("$.message")
                        .value("리소스가 성공적으로 생성되었습니다."));

        verify(memberProfileService).createProfile(eq(1L), any());
    }

    @Test
    @DisplayName("기본정보 입력값이 올바르지 않으면 400 Bad Request를 반환한다")
    void rejectInvalidInput() throws Exception {
        mockMvc.perform(post("/api/v1/members/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "John1",
                                  "age": 0,
                                  "height": 99.9,
                                  "weight": 200.1,
                                  "fullBodyImageValidationId": 0,
                                  "priceAlertEnabled": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("이미 기본정보가 등록되어 있으면 409 Conflict를 반환한다")
    void rejectDuplicatedProfile() throws Exception {
        given(memberProfileService.createProfile(any(), any()))
                .willThrow(new BusinessException(
                        ErrorCode.MEMBER_PROFILE_ALREADY_EXISTS
                ));

        mockMvc.perform(post("/api/v1/members/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("MEMBER_PROFILE_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("전신사진 검증 정보를 사용할 수 없으면 400 Bad Request를 반환한다")
    void rejectInvalidFullBodyImage() throws Exception {
        given(memberProfileService.createProfile(any(), any()))
                .willThrow(new BusinessException(
                        ErrorCode.INVALID_FULL_BODY_IMAGE
                ));

        mockMvc.perform(post("/api/v1/members/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_FULL_BODY_IMAGE"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    private String validRequestBody() {
        return """
                {
                  "name": "John Doe",
                  "age": 29,
                  "height": 175.5,
                  "weight": 70.3,
                  "fullBodyImageValidationId": 15,
                  "priceAlertEnabled": true
                }
                """;
    }
}
