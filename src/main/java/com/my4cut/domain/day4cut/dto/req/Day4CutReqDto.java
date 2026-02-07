package com.my4cut.domain.day4cut.dto.req;

import com.my4cut.domain.day4cut.enums.EmojiType;

import java.time.LocalDate;
import java.util.List;

/**
 * 하루네컷 요청 DTO 클래스.
 */
public class Day4CutReqDto {

    /**
     * 하루네컷 생성 요청 DTO
     */
    public record CreateReqDto(
            LocalDate date,
            String content,
            EmojiType emojiType,
            List<ImageReqDto> images
    ) {}

    /**
     * 하루네컷 수정 요청 DTO
     */
    public record UpdateReqDto(
            LocalDate date,
            String content,
            EmojiType emojiType,
            List<ImageReqDto> images
    ) {}

    /**
     * 이미지 요청 DTO
     */
    public record ImageReqDto(
            Long mediaFileId,
            Boolean isThumbnail
    ) {}
}
