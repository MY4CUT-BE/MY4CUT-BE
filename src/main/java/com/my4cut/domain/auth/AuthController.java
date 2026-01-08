package com.my4cut.domain.auth;

import com.my4cut.domain.auth.dto.LoginRequest;
import com.my4cut.domain.auth.dto.LoginResponse;
import com.my4cut.global.response.ApiResponse;
import com.my4cut.global.response.SuccessCode;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @RequestBody LoginRequest request
    ) {
        String token = authService.login(request);
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                new LoginResponse(token)
        );
    }

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                "OK"
        );
    }
}
