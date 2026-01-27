package com.my4cut.domain.album.controller;

import com.my4cut.domain.album.dto.AlbumRequestDto;
import com.my4cut.domain.album.dto.AlbumResponseDto;
import com.my4cut.domain.album.enums.AlbumSuccessCode;
import com.my4cut.domain.album.service.AlbumService;
import com.my4cut.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Album", description = "앨범 관리 API")
@RestController
@RequestMapping("/albums")
@RequiredArgsConstructor
public class AlbumController {

    private final AlbumService albumService;

    @Operation(summary = "앨범 생성", description = "새로운 앨범을 생성합니다.")
    @PostMapping
    public ApiResponse<AlbumResponseDto.Info> createAlbum(
            @Valid @RequestBody AlbumRequestDto.CreateOrUpdate requestDto,
            @AuthenticationPrincipal Long userId) {
        AlbumResponseDto.Info result = albumService.createAlbum(requestDto, userId);
        return ApiResponse.onSuccess(AlbumSuccessCode.ALBUM_CREATE_SUCCESS, result);
    }

    @Operation(summary = "내 앨범 목록 조회", description = "로그인한 사용자의 모든 앨범 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<List<AlbumResponseDto.Info>> getMyAlbums(
            @AuthenticationPrincipal Long userId) {
        List<AlbumResponseDto.Info> result = albumService.getMyAlbums(userId);
        return ApiResponse.onSuccess(AlbumSuccessCode.ALBUM_LIST_GET_SUCCESS, result);
    }

    @Operation(summary = "앨범 상세 조회", description = "특정 앨범의 상세 정보와 사진 목록을 조회합니다.")
    @GetMapping("/{albumId}")
    public ApiResponse<AlbumResponseDto.Detail> getAlbumDetail(
            @PathVariable Long albumId,
            @AuthenticationPrincipal Long userId) {
        AlbumResponseDto.Detail result = albumService.getAlbumDetail(albumId, userId);
        return ApiResponse.onSuccess(AlbumSuccessCode.ALBUM_DETAIL_GET_SUCCESS, result);
    }

    @Operation(summary = "앨범 이름 수정", description = "앨범의 이름을 수정합니다.")
    @PatchMapping("/{albumId}")
    public ApiResponse<AlbumResponseDto.Info> updateAlbumName(
            @PathVariable Long albumId,
            @Valid @RequestBody AlbumRequestDto.CreateOrUpdate requestDto,
            @AuthenticationPrincipal Long userId) {
        AlbumResponseDto.Info result = albumService.updateAlbumName(albumId, requestDto, userId);
        return ApiResponse.onSuccess(AlbumSuccessCode.ALBUM_UPDATE_SUCCESS, result);
    }

    @Operation(summary = "앨범 삭제", description = "특정 앨범을 삭제합니다.")
    @DeleteMapping("/{albumId}")
    public ApiResponse<Void> deleteAlbum(
            @PathVariable Long albumId,
            @AuthenticationPrincipal Long userId) {
        albumService.deleteAlbum(albumId, userId);
        return ApiResponse.onSuccess(AlbumSuccessCode.ALBUM_DELETE_SUCCESS, null);
    }

    @Operation(summary = "앨범에 사진 추가", description = "특정 사진들을 앨범에 추가합니다.")
    @PostMapping("/{albumId}/photos")
    public ApiResponse<AlbumResponseDto.Detail> addPhotosToAlbum(
            @PathVariable Long albumId,
            @Valid @RequestBody AlbumRequestDto.UpdatePhotos requestDto,
            @AuthenticationPrincipal Long userId) {
        AlbumResponseDto.Detail result = albumService.addPhotosToAlbum(albumId, requestDto, userId);
        return ApiResponse.onSuccess(AlbumSuccessCode.ALBUM_PHOTO_UPDATE_SUCCESS, result);
    }

    @Operation(summary = "앨범에서 사진 제외", description = "특정 사진들을 앨범에서 제외합니다.")
    @DeleteMapping("/{albumId}/photos")
    public ApiResponse<AlbumResponseDto.Detail> removePhotosFromAlbum(
            @PathVariable Long albumId,
            @Valid @RequestBody AlbumRequestDto.UpdatePhotos requestDto,
            @AuthenticationPrincipal Long userId) {
        AlbumResponseDto.Detail result = albumService.removePhotosFromAlbum(albumId, requestDto, userId);
        return ApiResponse.onSuccess(AlbumSuccessCode.ALBUM_PHOTO_UPDATE_SUCCESS, result);
    }
}
