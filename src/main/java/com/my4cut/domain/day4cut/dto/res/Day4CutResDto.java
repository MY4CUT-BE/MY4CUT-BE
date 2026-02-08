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
            List<String> viewUrls,
            String content,
            EmojiType emojiType
    ) {
        public static DetailResDto of(Day4Cut day4Cut, List<String> viewUrls) {
            return new DetailResDto(
                    day4Cut.getId(),
                    viewUrls,
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
            List<CalendarDayDto> dates
    ) {
        public record CalendarDayDto(
                int day,
                String thumbnailUrl
        ) {
            public static CalendarDayDto of(int day, String thumbnailUrl) {
                return new CalendarDayDto(day, thumbnailUrl);
            }
        }
    }
}
