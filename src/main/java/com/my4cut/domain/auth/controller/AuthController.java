package com.my4cut.domain.auth.controller;

import com.my4cut.domain.user.dto.UserReqDTO;
import com.my4cut.domain.user.dto.UserResDTO;
import com.my4cut.domain.auth.service.AuthService;
import com.my4cut.domain.user.entity.User;
import com.my4cut.global.response.ApiResponse;
import com.my4cut.global.response.SuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ApiResponse<Void> signup(
            @RequestBody @Valid UserReqDTO.SignUpDTO dto
    ) {
        authService.signup(dto);
        return ApiResponse.onSuccess(SuccessCode.CREATED, null);
    }

    @PostMapping("/login")
    public ApiResponse<UserResDTO.LoginDTO> login(
            @RequestBody @Valid UserReqDTO.LoginDTO dto
    ) {
        return ApiResponse.onSuccess(SuccessCode.OK, authService.login(dto));
    }

    @PostMapping("/refresh")
    public ApiResponse<UserResDTO.LoginDTO> refresh(
            @RequestHeader("Authorization") String authHeader
    ) {
        // Bearer 제거
        String refreshToken = authHeader.substring(7);

        return ApiResponse.onSuccess(
                SuccessCode.OK,
                authService.refresh(refreshToken)
        );
    }

    @DeleteMapping("/withdraw")
    public ApiResponse<Void> withdraw(
            @AuthenticationPrincipal Long userId
    ) {
        authService.withdraw(userId);
        return ApiResponse.onSuccess(SuccessCode.OK, null);
    }

}
