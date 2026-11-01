package com.ktb.lookddak.domain.image.controller;

import com.ktb.lookddak.domain.image.dto.FullBodyImageValidationResponse;
import com.ktb.lookddak.domain.image.dto.BodyImageWarmupResponse;
import com.ktb.lookddak.domain.image.entity.FullBodyImageValidation;
import com.ktb.lookddak.domain.image.exception.BodyImageValidationException;
import com.ktb.lookddak.domain.image.service.FullBodyImageValidationService;
import com.ktb.lookddak.domain.image.service.BodyImageWarmupService;
import com.ktb.lookddak.domain.member.entity.Member;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FullBodyImageValidationControllerTest {

    @Mock
    private FullBodyImageValidationService validationService;
    @Mock
    private BodyImageWarmupService warmupService;
    @Mock
    private MemberPrincipal principal;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        FullBodyImageValidationController controller =
                new FullBodyImageValidationController(
                        validationService,
                        warmupService
                );
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(
                        new AuthenticationPrincipalArgumentResolver()
                )
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

    @Test
    @DisplayName("Warm-up 접수 요청은 202를 반환한다")
    void acceptWarmupRequest() throws Exception {
        given(warmupService.requestWarmup()).willReturn(
                BodyImageWarmupResponse.accepted(
                        "BODY_IMAGE_WARMUP_ACCEPTED",
                        "전신 이미지 검증 사전 활성화 요청을 접수했습니다."
                )
        );

        mockMvc.perform(post("/api/v1/full-body-image/warmup"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.code")
                        .value("BODY_IMAGE_WARMUP_ACCEPTED"))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(warmupService).requestWarmup();
    }

    @Test
    @DisplayName("Warm-up 제한 시 Retry-After를 그대로 반환한다")
    void returnRetryAfterWhenWarmupIsRateLimited() throws Exception {
        given(warmupService.requestWarmup()).willReturn(
                BodyImageWarmupResponse.rateLimited(
                        "BODY_IMAGE_WARMUP_RATE_LIMITED",
                        "사전 활성화 요청이 너무 많습니다. 잠시 후 다시 시도해주세요.",
                        12L
                )
        );

        mockMvc.perform(post("/api/v1/full-body-image/warmup"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "12"))
                .andExpect(jsonPath("$.code")
                        .value("BODY_IMAGE_WARMUP_RATE_LIMITED"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("전신사진 검증에 성공하면 검증 ID와 이미지 URL을 반환한다")
    void validateImage() throws Exception {
        MockMultipartFile image = image();
        given(validationService.validate(eq(1L), any()))
                .willReturn(response());

        mockMvc.perform(multipart("/api/v1/full-body-image/validate")
                        .file(image))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.validationId").value(15))
                .andExpect(jsonPath("$.data.fullBodyImageUrl")
                        .value("https://s3.example.com/validated.png"))
                .andExpect(jsonPath("$.message")
                        .value("요청이 성공적으로 처리되었습니다."));

        verify(validationService).validate(eq(1L), any());
    }

    @Test
    @DisplayName("AI 사용자 검증 실패 코드와 메시지를 400으로 반환한다")
    void rejectInvalidBodyImage() throws Exception {
        MockMultipartFile image = image();
        given(validationService.validate(eq(1L), any()))
                .willThrow(new BodyImageValidationException(
                        "FULL_BODY_NOT_VISIBLE",
                        "머리부터 발끝까지 모두 나오도록 촬영해주세요."
                ));

        mockMvc.perform(multipart("/api/v1/full-body-image/validate")
                        .file(image))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("FULL_BODY_NOT_VISIBLE"))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.message")
                        .value("머리부터 발끝까지 모두 나오도록 촬영해주세요."));
    }

    @Test
    @DisplayName("이미지 파일을 전달하지 않으면 400을 반환한다")
    void rejectMissingImagePart() throws Exception {
        mockMvc.perform(multipart("/api/v1/full-body-image/validate"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("지원하지 않는 파일은 400을 반환한다")
    void rejectUnsupportedImage() throws Exception {
        MockMultipartFile image = image();
        given(validationService.validate(eq(1L), any()))
                .willThrow(new BusinessException(
                        ErrorCode.IMAGE_FORMAT_UNSUPPORTED
                ));

        mockMvc.perform(multipart("/api/v1/full-body-image/validate")
                        .file(image))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("IMAGE_FORMAT_UNSUPPORTED"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    private MockMultipartFile image() {
        return new MockMultipartFile(
                "image",
                "body.png",
                "image/png",
                new byte[]{1}
        );
    }

    private FullBodyImageValidationResponse response() {
        Member member = Member.create(
                "member@test.com",
                "encoded-password"
        );
        FullBodyImageValidation validation =
                FullBodyImageValidation.create(
                        member,
                        "full-body/1/validated.png"
                );
        ReflectionTestUtils.setField(validation, "id", 15L);

        return FullBodyImageValidationResponse.of(
                validation,
                "https://s3.example.com/validated.png"
        );
    }
}
