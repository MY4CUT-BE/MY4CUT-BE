package com.my4cut.domain.auth;

import com.my4cut.global.response.ApiResponse;
import com.my4cut.global.response.SuccessCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestAuthController {

    @GetMapping("/auth/test")
    public ApiResponse<String> authTest() {
        return ApiResponse.onSuccess(SuccessCode.OK, "AUTH OK");
    }
}
