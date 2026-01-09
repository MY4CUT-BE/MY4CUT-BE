package com.my4cut.domain.user.controller;

import com.my4cut.domain.user.dto.UserReqDTO;
import com.my4cut.domain.user.dto.UserResDTO;
import com.my4cut.domain.user.service.AuthService;
import com.my4cut.global.response.ApiResponse;
import com.my4cut.global.response.SuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    // 회원가입
//    @PostMapping("/sign-up")
//    public ApiResponse<UserResDTO.JoinDTO> signUp(
//            @RequestBody @Valid MemberReqDTO.JoinDTO dto
//    ){
//        return ApiResponse.onSuccess(MemberSuccessCode.FOUND, authService.signup(dto));
//    }

    // 로그인
    @PostMapping("/auth/login")
    public ApiResponse<UserResDTO.LoginDTO> login(
            @RequestBody @Valid UserReqDTO.LoginDTO dto
    ){
        return ApiResponse.onSuccess(SuccessCode.OK, authService.login(dto));
    }
}