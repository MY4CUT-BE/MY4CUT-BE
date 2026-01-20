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

    /**
     * Register a new user account.
     *
     * @param dto the sign-up request containing user credentials and profile information
     * @return an ApiResponse with SuccessCode.CREATED and a null body
     */
    @PostMapping("/signup")
    public ApiResponse<Void> signup(
            @RequestBody @Valid UserReqDTO.SignUpDTO dto
    ) {
        authService.signup(dto);
        return ApiResponse.onSuccess(SuccessCode.CREATED, null);
    }

    /**
     * Authenticate a user and produce a login response.
     *
     * @param dto the login credentials (e.g., username/email and password)
     * @return an ApiResponse whose payload is a UserResDTO.LoginDTO containing authentication tokens and user information
     */
    @PostMapping("/login")
    public ApiResponse<UserResDTO.LoginDTO> login(
            @RequestBody @Valid UserReqDTO.LoginDTO dto
    ) {
        return ApiResponse.onSuccess(SuccessCode.OK, authService.login(dto));
    }

    /**
     * Refreshes authentication using a bearer refresh token and returns new login tokens.
     *
     * @param authHeader the HTTP `Authorization` header containing a Bearer token in the form `Bearer <refreshToken>`
     * @return an `ApiResponse` containing a `UserResDTO.LoginDTO` with refreshed authentication tokens
     */
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

    /**
     * Delete the account of the authenticated user.
     *
     * @param userId ID of the authenticated user extracted from the security principal
     * @return an ApiResponse with success code OK and no body
     */
    @DeleteMapping("/withdraw")
    public ApiResponse<Void> withdraw(
            @AuthenticationPrincipal Long userId
    ) {
        authService.withdraw(userId);
        return ApiResponse.onSuccess(SuccessCode.OK, null);
    }

}