package com.my4cut.domain.pose.dto.res;

import com.my4cut.domain.pose.entity.Pose;
import lombok.Builder;
import lombok.Getter;

public class PoseResDto {

    @Getter
    @Builder
    public static class PoseListResDto {
        private Long poseId;
        private String title;
        private String fileKey;
        private String viewUrl;
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
    public static class PoseDetailResDto {
        private Long poseId;
        private String title;
        private String fileKey;
        private String viewUrl;
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
    public static class BookmarkResDto {
        private Boolean success;

        public static BookmarkResDto of(Boolean success) {
            return BookmarkResDto.builder()
                    .success(success)
                    .build();
        }
    }
}
