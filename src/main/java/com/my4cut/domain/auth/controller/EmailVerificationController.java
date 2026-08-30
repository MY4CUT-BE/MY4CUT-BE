package com.my4cut.domain.auth.controller;

import com.my4cut.domain.auth.dto.req.EmailSendReqDto;
import com.my4cut.domain.auth.dto.req.EmailVerifyReqDto;
import com.my4cut.domain.auth.dto.res.EmailVerifyResDto;
import com.my4cut.domain.auth.service.EmailVerificationRequestService;
import com.my4cut.global.response.ApiResponse;
import com.my4cut.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원가입과 비밀번호 재설정을 위한 이메일 인증코드 발송 및 검증 API를 제공한다.
 */
@Tag(name = "이메일 인증", description = "회원가입 및 비밀번호 재설정 이메일 인증 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/email")
public class EmailVerificationController {

    private final EmailVerificationRequestService emailVerificationRequestService;

    @Operation(
            summary = "회원가입 이메일 인증코드 발송",
            description = "가입되지 않았거나 탈퇴 처리된 이메일로 회원가입용 6자리 인증코드를 발송합니다."
    )
    @PostMapping("/signup/send")
    public ApiResponse<Void> sendSignupCode(
            @Valid @RequestBody EmailSendReqDto request,
            HttpServletRequest httpRequest
    ) {
        emailVerificationRequestService.sendSignupCode(request.email(), httpRequest.getRemoteAddr());
        return ApiResponse.onSuccess(SuccessCode.OK);
    }

    @Operation(
            summary = "회원가입 이메일 인증코드 검증",
            description = "회원가입용 인증코드를 검증하고 회원가입 인증 완료 상태를 저장합니다."
    )
    @PostMapping("/signup/verify")
    public ApiResponse<EmailVerifyResDto> verifySignupCode(
            @Valid @RequestBody EmailVerifyReqDto request,
            HttpServletRequest httpRequest
    ) {
        String verificationToken = emailVerificationRequestService.verifySignupCode(
                request.email(),
                request.code(),
                httpRequest.getRemoteAddr()
        );
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                new EmailVerifyResDto(verificationToken)
        );
    }

    @Operation(
            summary = "비밀번호 재설정 이메일 인증코드 발송",
            description = "가입된 이메일 로그인 계정에만 비밀번호 재설정용 6자리 인증코드를 발송합니다."
    )
    @PostMapping("/password-reset/send")
    public ApiResponse<Void> sendPasswordResetCode(
            @Valid @RequestBody EmailSendReqDto request,
            HttpServletRequest httpRequest
    ) {
        emailVerificationRequestService.sendPasswordResetCode(
                request.email(),
                httpRequest.getRemoteAddr()
        );
        return ApiResponse.onSuccess(SuccessCode.OK);
    }

    @Operation(
            summary = "비밀번호 재설정 이메일 인증코드 검증",
            description = "비밀번호 재설정용 인증코드를 검증하고 비밀번호 재설정 인증 완료 상태를 저장합니다."
    )
    @PostMapping("/password-reset/verify")
    public ApiResponse<EmailVerifyResDto> verifyPasswordResetCode(
            @Valid @RequestBody EmailVerifyReqDto request,
            HttpServletRequest httpRequest
    ) {
        String verificationToken = emailVerificationRequestService.verifyPasswordResetCode(
                request.email(),
                request.code(),
                httpRequest.getRemoteAddr()
        );
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                new EmailVerifyResDto(verificationToken)
        );
    }
}
