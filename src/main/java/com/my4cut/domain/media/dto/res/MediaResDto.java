package com.my4cut.domain.media.dto.res;

import com.my4cut.domain.media.entity.MediaFile;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

public class MediaResDto {

    @Getter
    @Builder
    public static class UploadResDto {
        private Long fileId;
        private String fileUrl;

        public static UploadResDto of(MediaFile mediaFile) {
            return UploadResDto.builder()
                    .fileId(mediaFile.getId())
                    .fileUrl(mediaFile.getFileUrl())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class MediaListResDto {
        private Long mediaId;

        public static MediaListResDto of(MediaFile mediaFile) {
            return MediaListResDto.builder()
                    .mediaId(mediaFile.getId())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class MediaDetailResDto {
        private Long mediaId;
        private String fileUrl;
        private String mediaType;
        private String diary;
        private LocalDate takenDate;
        private Boolean isFinal;

        public static MediaDetailResDto of(MediaFile mediaFile) {
            return MediaDetailResDto.builder()
                    .mediaId(mediaFile.getId())
                    .fileUrl(mediaFile.getFileUrl())
                    .mediaType(mediaFile.getMediaType().name())
                    .diary(mediaFile.getDiary())
                    .takenDate(mediaFile.getTakenDate())
                    .isFinal(mediaFile.getIsFinal())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class DeleteResDto {
        private Boolean success;

        public static DeleteResDto of(Boolean success) {
            return DeleteResDto.builder()
                    .success(success)
                    .build();
        }
    }
}
