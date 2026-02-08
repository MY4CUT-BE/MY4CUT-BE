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
        private String fileKey;
        private String viewUrl;

        public static UploadResDto of(MediaFile mediaFile, String viewUrl) {
            return UploadResDto.builder()
                    .fileId(mediaFile.getId())
                    .fileKey(mediaFile.getFileUrl())
                    .viewUrl(viewUrl)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class MediaListResDto {
        private Long mediaId;
        private String fileKey;
        private String viewUrl;

        public static MediaListResDto of(MediaFile mediaFile, String viewUrl) {
            return MediaListResDto.builder()
                    .mediaId(mediaFile.getId())
                    .fileKey(mediaFile.getFileUrl())
                    .viewUrl(viewUrl)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class MediaDetailResDto {
        private Long mediaId;
        private String fileKey;
        private String viewUrl;
        private String mediaType;
        private String diary;
        private LocalDate takenDate;
        private Boolean isFinal;

        public static MediaDetailResDto of(MediaFile mediaFile, String viewUrl) {
            return MediaDetailResDto.builder()
                    .mediaId(mediaFile.getId())
                    .fileKey(mediaFile.getFileUrl())
                    .viewUrl(viewUrl)
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
