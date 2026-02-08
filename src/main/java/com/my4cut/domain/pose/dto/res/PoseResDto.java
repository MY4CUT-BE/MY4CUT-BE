package com.my4cut.domain.pose.dto.res;

import com.my4cut.domain.pose.entity.Pose;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

public class PoseResDto {

    @Getter
    @Builder
    @Schema(description = "포즈 목록 조회 응답 DTO")
    public static class PoseListResDto {
        @Schema(description = "포즈 ID", example = "1")
        private Long poseId;

        @Schema(description = "포즈 제목", example = "귀여운 고양이 포즈")
        private String title;

        @Schema(description = "파일 키 (S3 경로)", example = "pose/cat_pose.jpg")
        private String fileKey;

        @Schema(description = "실제 조회 가능한 URL (10분 유효)", example = "https://s3...")
        private String viewUrl;

        @Schema(description = "추천 인원수", example = "2")
        private Integer peopleCount;

        public static PoseListResDto of(Pose pose, String viewUrl) {
            return PoseListResDto.builder()
                    .poseId(pose.getId())
                    .title(pose.getTitle())
                    .fileKey(pose.getImageUrl())
                    .viewUrl(viewUrl)
                    .peopleCount(pose.getPeopleCount())
                    .build();
        }
    }

    @Getter
    @Builder
    @Schema(description = "포즈 상세 조회 응답 DTO")
    public static class PoseDetailResDto {
        @Schema(description = "포즈 ID", example = "1")
        private Long poseId;

        @Schema(description = "포즈 제목", example = "귀여운 고양이 포즈")
        private String title;

        @Schema(description = "파일 키 (S3 경로)", example = "pose/cat_pose.jpg")
        private String fileKey;

        @Schema(description = "실제 조회 가능한 URL (10분 유효)", example = "https://s3...")
        private String viewUrl;

        @Schema(description = "추천 인원수", example = "2")
        private Integer peopleCount;

        public static PoseDetailResDto of(Pose pose, String viewUrl) {
            return PoseDetailResDto.builder()
                    .poseId(pose.getId())
                    .title(pose.getTitle())
                    .fileKey(pose.getImageUrl())
                    .viewUrl(viewUrl)
                    .peopleCount(pose.getPeopleCount())
                    .build();
        }
    }

    @Getter
    @Builder
    @Schema(description = "즐겨찾기 결과 응답 DTO")
    public static class BookmarkResDto {
        @Schema(description = "성공 여부", example = "true")
        private Boolean success;

        public static BookmarkResDto of(Boolean success) {
            return BookmarkResDto.builder()
                    .success(success)
                    .build();
        }
    }
}
