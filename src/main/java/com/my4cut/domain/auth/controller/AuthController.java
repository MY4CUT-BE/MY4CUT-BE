package com.my4cut.domain.auth.controller;

import com.my4cut.domain.auth.dto.AuthReqDTO;
import com.my4cut.domain.user.dto.UserReqDTO;
import com.my4cut.domain.user.dto.UserResDTO;
import com.my4cut.domain.auth.service.AuthService;
import com.my4cut.global.response.ApiResponse;
import com.my4cut.global.response.ErrorCode;
import com.my4cut.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@Tag(name = "Auth", description = "인증/인가 관련 API")
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "회원가입",
            description = "email, password, nickname으로 회원가입을 수행합니다."
    )
    @PostMapping("/signup")
    public ApiResponse<Void> signup(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = UserReqDTO.SignUpDTO.class)
                    )
            )
            @RequestBody @Valid UserReqDTO.SignUpDTO dto
    ) {
        authService.signup(dto);
        return ApiResponse.onSuccess(SuccessCode.CREATED, null);
    }

    @Operation(
            summary = "로그인",
            description = "이메일과 비밀번호로 로그인합니다."
    )
    @PostMapping("/login")
    public ApiResponse<UserResDTO.LoginDTO> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = UserReqDTO.LoginDTO.class)
                    )
            )
            @RequestBody @Valid UserReqDTO.LoginDTO dto
    ) {
        return ApiResponse.onSuccess(SuccessCode.OK, authService.login(dto));
    }

    @Operation(
            summary = "토큰 재발급",
            description = "Refresh Token을 이용해 Access Token을 재발급합니다."
    )
    @PostMapping("/refresh")
    public ApiResponse<UserResDTO.LoginDTO> refresh(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        if (authHeader == null || !authHeader.startsWith("Bearer ") || authHeader.length() <= 7) {
            return ApiResponse.onFailure(
                    ErrorCode.UNAUTHORIZED,
                    null
            );
        }

        String refreshToken = authHeader.substring(7);

        return ApiResponse.onSuccess(
                SuccessCode.OK,
                authService.refresh(refreshToken)
        );
    }

    @Operation(
            summary = "회원 탈퇴",
            description = "현재 로그인한 사용자의 계정을 탈퇴 처리합니다."
    )
    @DeleteMapping("/withdraw")
    public ApiResponse<Void> withdraw(
            @AuthenticationPrincipal Long userId
    ) {
        authService.withdraw(userId);
        return ApiResponse.onSuccess(SuccessCode.OK, null);
    }

    @Operation(
            summary = "카카오 로그인",
            description = "카카오 Access Token을 이용해 로그인합니다."
    )
    @PostMapping("/kakao")
    public ApiResponse<UserResDTO.LoginDTO> kakaoLogin(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = AuthReqDTO.KakaoLoginReqDto.class)
                    )
            )
            @RequestBody AuthReqDTO.KakaoLoginReqDto dto
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                authService.kakaoLogin(dto.accessToken())
        );
    }



}
