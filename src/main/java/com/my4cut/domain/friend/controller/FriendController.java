package com.my4cut.domain.friend.controller;

import com.my4cut.domain.friend.dto.req.FriendRequestReqDto;
import com.my4cut.domain.friend.dto.res.FriendRequestResDto;
import com.my4cut.domain.friend.dto.res.FriendResDto;
import com.my4cut.domain.friend.service.FriendService;
import com.my4cut.global.response.ApiResponse;
import com.my4cut.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Friend", description = "친구 관리 API")
@RestController
@RequestMapping("/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    //친구 추가 요청
    @Operation(
            summary = "친구 추가 요청",
            description = "친구 추가 요청을 보냅니다."
    )
    @PostMapping("/requests")
    public ApiResponse<FriendRequestResDto.SendRequestResDto> sendRequest(
            @AuthenticationPrincipal Long userId,
            @RequestBody FriendRequestReqDto dto
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                friendService.sendFriendRequest(userId, dto.targetFriendCode())
        );
    }

    //받은 요청 조회
    @Operation(
            summary = "친구 추가 요청 조회",
            description = "친구 추가 요청을 조회합니다."
    )
    @GetMapping("/requests")
    public ApiResponse<List<FriendRequestResDto.ReceivedRequestResDto>> receivedRequests(
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                friendService.getReceivedRequests(userId)
        );
    }

    //보낸 요청 취소
    @Operation(
            summary = "보낸 요청 취소",
            description = "보낸 요청을 취소합니다."
    )
    @DeleteMapping("/requests/{requestId}")
    public ApiResponse<Void> cancelRequest(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long requestId
    ) {
        friendService.cancelSentRequest(userId, requestId);
        return ApiResponse.onSuccess(SuccessCode.OK);
    }

    //요청 수락
    @Operation(
            summary = "친구 추가 요청 수락",
            description = "받은 친구 추가 요청을 수락합니다."
    )
    @PostMapping("/requests/{requestId}/accept")
    public ApiResponse<FriendRequestResDto.AcceptRequestResDto> acceptRequest(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long requestId
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                friendService.acceptFriendRequest(userId, requestId)
        );
    }

    //요청 거절
    @Operation(
            summary = "친구 추가 요청 거절",
            description = "받은 친구 추가 요청을 거절합니다."
    )
    @PostMapping("/requests/{requestId}/reject")
    public ApiResponse<FriendRequestResDto.RejectRequestResDto> rejectRequest(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long requestId
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                friendService.rejectFriendRequest(userId, requestId)
        );
    }

    //친구 즐겨찾기
    @Operation(
            summary = "친구 즐겨찾기",
            description = "친구를 즐겨찾기합니다."
    )
    @PostMapping("/{id}/favorites")
    public ApiResponse<FriendResDto.FavoriteFriendResDto> favoriteFriend(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                friendService.favoriteFriend(userId, id)
        );
    }

    //친구 즐겨찾기 해제
    @Operation(
            summary = "친구 즐겨찾기 해제",
            description = "친구 즐겨찾기를 해제합니다."
    )
    @DeleteMapping("/{id}/favorites")
    public ApiResponse<FriendResDto.FavoriteFriendResDto> unfavoriteFriend(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                friendService.unfavoriteFriend(userId, id)
        );
    }

    // 내 친구 목록 조회
    @Operation(
            summary = "내 친구 목록 조회",
            description = "내 친구 목록을 조회합니다."
    )
    @GetMapping
    public ApiResponse<List<FriendResDto>> getMyFriends(
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                friendService.getMyFriends(userId)
        );
    }

    // 친구 삭제
    @Operation(
            summary = "친구 삭제",
            description = "친구를 삭제합니다."
    )
    @DeleteMapping("/{friendId}")
    public ApiResponse<Void> deleteFriend(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long friendId
    ) {
        friendService.deleteFriend(userId, friendId);
        return ApiResponse.onSuccess(SuccessCode.OK);
    }
}

