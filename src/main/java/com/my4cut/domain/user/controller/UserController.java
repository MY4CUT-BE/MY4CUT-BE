package com.my4cut.domain.user.controller;

import com.my4cut.domain.user.dto.UserReqDTO;
import com.my4cut.domain.user.dto.UserResDTO;
import com.my4cut.domain.user.service.UserService;
import com.my4cut.global.response.ApiResponse;
import com.my4cut.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
@Tag(name = "User", description = "사용자 정보 및 마이페이지 API")
public class UserController {

    private final UserService userService;

    //마이페이지
    @Operation(
            summary = "내 정보 조회 (마이페이지)",
            description = "현재 로그인한 사용자의 마이페이지 정보를 조회합니다."
    )
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

    //닉네임 변경
    @Operation(
            summary = "닉네임 변경",
            description = "현재 로그인한 사용자의 닉네임을 변경합니다."
    )
    @PatchMapping("/me/nickname")
    public ApiResponse<UserResDTO.UpdateNicknameDTO> updateNickname(
            @AuthenticationPrincipal Long userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = UserReqDTO.UpdateNicknameDTO.class)
                    )
            )
            @RequestBody @Valid UserReqDTO.UpdateNicknameDTO request
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                userService.updateNickname(userId, request)
        );
    }

    // 프로필 사진 변경
    @Operation(
            summary = "프로필 이미지 변경",
            description = """
                    사용자의 프로필 이미지를 변경합니다.
                    Presigned URL로 업로드된 이미지의 fileKey를 서버에 전달합니다.
                    """
    )
    @PatchMapping("/me/image")
    public ApiResponse<UserResDTO.UpdateProfileImageDTO> updateProfileImage(
            @AuthenticationPrincipal Long userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = UserReqDTO.UpdateProfileImageDTO.class)
                    )
            )
            @RequestBody @Valid UserReqDTO.UpdateProfileImageDTO request
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                userService.updateProfileImage(userId, request)
        );
    }
}
