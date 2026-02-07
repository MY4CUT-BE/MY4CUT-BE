package com.my4cut.domain.day4cut.dto.res;

import com.my4cut.domain.day4cut.entity.Day4Cut;
import com.my4cut.domain.day4cut.entity.Day4CutImage;
import com.my4cut.domain.day4cut.enums.EmojiType;

import java.util.List;

/**
 * 하루네컷 응답 DTO 클래스.
 */
public class Day4CutResDto {

    /**
     * 하루네컷 생성 응답 DTO
     */
    public record CreateResDto(
            Boolean success
    ) {
        public static CreateResDto of() {
            return new CreateResDto(true);
        }
    }

    /**
     * 하루네컷 조회 응답 DTO
     */
    public record DetailResDto(
            Long id,
            List<String> fileUrl,
            String content,
            EmojiType emojiType
    ) {
        public static DetailResDto from(Day4Cut day4Cut) {
            List<String> fileUrls = day4Cut.getImages().stream()
                    .map(image -> image.getMediaFile().getFileUrl())
                    .toList();

            return new DetailResDto(
                    day4Cut.getId(),
                    fileUrls,
                    day4Cut.getContent(),
                    day4Cut.getEmojiType()
            );
        }
    }

    /**
     * 하루네컷 수정 응답 DTO
     */
    public record UpdateResDto(
            Boolean success
    ) {
        public static UpdateResDto of() {
            return new UpdateResDto(true);
        }
    }

    /**
     * 하루네컷 삭제 응답 DTO
     */
    public record DeleteResDto(
            Boolean success
    ) {
        public static DeleteResDto of() {
            return new DeleteResDto(true);
        }
    }

    /**
     * 캘린더 조회 응답 DTO
     */
    public record CalendarResDto(
            List<Integer> dates
    ) {
        public static CalendarResDto of(List<Integer> dates) {
            return new CalendarResDto(dates);
        }
    }
}
