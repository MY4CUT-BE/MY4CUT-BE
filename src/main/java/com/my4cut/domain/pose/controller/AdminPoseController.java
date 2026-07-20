package com.my4cut.domain.pose.controller;

import com.my4cut.domain.pose.dto.req.PoseCreateRequest;
import com.my4cut.domain.pose.dto.req.PoseUpdateRequest;
import com.my4cut.domain.pose.service.PoseAdminService;
import com.my4cut.global.response.ApiResponse;
import com.my4cut.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/poses")
@RequiredArgsConstructor
@Tag(name = "Admin Pose", description = "관리자 전용 포즈 관리 API")
public class AdminPoseController {

    private final PoseAdminService poseAdminService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "포즈 생성")
    public ApiResponse<Void> createPose(
            @Valid @RequestPart("metadata") PoseCreateRequest metadata,
            @RequestPart("image") MultipartFile image
    ) {
        poseAdminService.createPose(metadata, image);
        return ApiResponse.onSuccess(SuccessCode.CREATED);
    }

    @PatchMapping(path = "/{poseId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "포즈 수정")
    public ApiResponse<Void> updatePose(
            @PathVariable Long poseId,
            @Valid @RequestPart(value = "metadata", required = false) PoseUpdateRequest metadata,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        poseAdminService.updatePose(poseId, metadata, image);
        return ApiResponse.onSuccess(SuccessCode.OK);
    }

    @DeleteMapping("/{poseId}")
    @Operation(summary = "포즈 삭제")
    public ApiResponse<Void> deletePose(@PathVariable Long poseId) {
        poseAdminService.deletePose(poseId);
        return ApiResponse.onSuccess(SuccessCode.OK);
    }
}
