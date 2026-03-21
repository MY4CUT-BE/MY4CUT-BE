package com.my4cut.domain.auth.controller;

import com.my4cut.domain.auth.dto.req.EmailSendReqDto;
import com.my4cut.domain.auth.dto.req.EmailVerifyReqDto;
import com.my4cut.domain.auth.service.EmailVerificationService;
import com.my4cut.global.response.ApiResponse;
import com.my4cut.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * 이메일 인증코드 발송 및 검증 API를 제공한다.
 */
@Tag(name = "Email Verification", description = "이메일 인증 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/email")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    @Operation(summary = "인증코드 발송", description = "입력한 이메일로 6자리 인증코드를 발송한다.")
    @PostMapping("/send")
    public ApiResponse<Void> send(@Valid @RequestBody EmailSendReqDto request) {
        emailVerificationService.sendCode(request.email());
        return ApiResponse.onSuccess(SuccessCode.OK);
    }

    @Operation(summary = "인증코드 검증", description = "이메일과 인증코드를 검증하고 인증 완료 상태를 저장한다.")
    @PostMapping("/verify")
    public ApiResponse<Void> verify(@Valid @RequestBody EmailVerifyReqDto request) {
        emailVerificationService.verifyCode(request.email(), request.code());
        return ApiResponse.onSuccess(SuccessCode.OK);
    }
}
