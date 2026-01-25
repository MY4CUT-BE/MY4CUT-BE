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
        private String imageUrl;
        private Integer peopleCount;

        public static PoseListResDto of(Pose pose) {
            return PoseListResDto.builder()
                    .poseId(pose.getId())
                    .title(pose.getTitle())
                    .imageUrl(pose.getImageUrl())
                    .peopleCount(pose.getPeopleCount())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class PoseDetailResDto {
        private Long poseId;
        private String title;
        private String imageUrl;
        private Integer peopleCount;

        public static PoseDetailResDto of(Pose pose) {
            return PoseDetailResDto.builder()
                    .poseId(pose.getId())
                    .title(pose.getTitle())
                    .imageUrl(pose.getImageUrl())
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
