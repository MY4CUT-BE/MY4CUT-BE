package com.my4cut.domain.user.controller;

import com.my4cut.domain.user.dto.UserReqDTO;
import com.my4cut.domain.user.dto.UserResDTO;
import com.my4cut.domain.user.service.UserService;
import com.my4cut.global.response.ApiResponse;
import com.my4cut.global.response.SuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserResDTO.MeDTO> getMyInfo() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        Long userId = (Long) authentication.getPrincipal();

        return ApiResponse.onSuccess(
                SuccessCode.OK,
                userService.getMyInfo(userId)
        );
    }

    @PatchMapping("/me/nickname")
    public ApiResponse<UserResDTO.UpdateNicknameDTO> updateNickname(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid UserReqDTO.UpdateNicknameDTO request
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                userService.updateNickname(userId, request)
        );
    }
}
