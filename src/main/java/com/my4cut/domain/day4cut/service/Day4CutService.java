package com.my4cut.domain.day4cut.service;

import com.my4cut.domain.day4cut.dto.req.Day4CutReqDto;
import com.my4cut.domain.day4cut.dto.res.Day4CutResDto;
import com.my4cut.domain.day4cut.entity.Day4Cut;
import com.my4cut.domain.day4cut.entity.Day4CutImage;
import com.my4cut.domain.day4cut.exception.Day4CutErrorCode;
import com.my4cut.domain.day4cut.exception.Day4CutException;
import com.my4cut.domain.day4cut.repository.Day4CutRepository;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.global.exception.BusinessException;
import com.my4cut.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 하루네컷 관련 비즈니스 로직을 처리하는 서비스 클래스.
 */
@Service
@RequiredArgsConstructor
public class Day4CutService {

    private final Day4CutRepository day4CutRepository;
    private final UserRepository userRepository;

    /**
     * 하루네컷을 생성한다.
     */
    @Transactional
    public Day4CutResDto.CreateResDto createDay4Cut(Long userId, Day4CutReqDto.CreateReqDto reqDto) {
        User user = findUserById(userId);

        // 이미지 검증
        validateImages(reqDto.images());

        // 내용 검증
        validateContent(reqDto.content());

        // 하루네컷 생성
        Day4Cut day4Cut = Day4Cut.builder()
                .user(user)
                .date(reqDto.date())
                .content(reqDto.content())
                .emojiType(reqDto.emojiType())
                .build();

        // 이미지 추가
        addImages(day4Cut, reqDto.images());

        day4CutRepository.save(day4Cut);

        return Day4CutResDto.CreateResDto.of();
    }

    /**
     * 하루네컷을 조회한다.
     */
    @Transactional(readOnly = true)
    public Day4CutResDto.DetailResDto getDay4Cut(Long userId, Long day4CutId) {
        User user = findUserById(userId);

        Day4Cut day4Cut = day4CutRepository.findByIdAndUser(day4CutId, user)
                .orElseThrow(() -> new Day4CutException(Day4CutErrorCode.DAY4CUT_NOT_FOUND));

        return Day4CutResDto.DetailResDto.from(day4Cut);
    }

    /**
     * 하루네컷을 수정한다.
     */
    @Transactional
    public Day4CutResDto.UpdateResDto updateDay4Cut(Long userId, Day4CutReqDto.UpdateReqDto reqDto) {
        User user = findUserById(userId);

        Day4Cut day4Cut = day4CutRepository.findByIdAndUser(reqDto.id(), user)
                .orElseThrow(() -> new Day4CutException(Day4CutErrorCode.DAY4CUT_NOT_FOUND));

        // 이미지 검증
        validateImages(reqDto.images());

        // 내용 검증
        validateContent(reqDto.content());

        // 하루네컷 정보 수정
        day4Cut.update(reqDto.content(), reqDto.emojiType());

        // 기존 이미지 삭제 후 새 이미지 추가 (전체 교체)
        day4Cut.clearImages();
        addImages(day4Cut, reqDto.images());

        return Day4CutResDto.UpdateResDto.of();
    }

    /**
     * 하루네컷을 삭제한다.
     */
    @Transactional
    public Day4CutResDto.DeleteResDto deleteDay4Cut(Long userId, Long day4CutId) {
        User user = findUserById(userId);

        Day4Cut day4Cut = day4CutRepository.findByIdAndUser(day4CutId, user)
                .orElseThrow(() -> new Day4CutException(Day4CutErrorCode.DAY4CUT_NOT_FOUND));

        day4CutRepository.delete(day4Cut);

        return Day4CutResDto.DeleteResDto.of();
    }

    /**
     * 사용자를 조회한다.
     */
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    /**
     * 이미지 목록을 검증한다.
     * - 이미지는 최소 1장 이상이어야 한다.
     * - 썸네일은 정확히 1개여야 한다.
     * - 이미지가 1장인 경우 자동으로 썸네일로 설정한다.
     */
    private void validateImages(List<Day4CutReqDto.ImageReqDto> images) {
        if (images == null || images.isEmpty()) {
            throw new Day4CutException(Day4CutErrorCode.DAY4CUT_IMAGES_REQUIRED);
        }

        // 이미지가 1장인 경우 썸네일 검증 생략 (자동 설정)
        if (images.size() == 1) {
            return;
        }

        // 썸네일 개수 검증
        long thumbnailCount = images.stream()
                .filter(img -> Boolean.TRUE.equals(img.isThumbnail()))
                .count();

        if (thumbnailCount != 1) {
            throw new Day4CutException(Day4CutErrorCode.DAY4CUT_INVALID_THUMBNAIL);
        }
    }

    /**
     * 내용을 검증한다.
     */
    private void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new Day4CutException(Day4CutErrorCode.DAY4CUT_CONTENT_REQUIRED);
        }
    }

    /**
     * 하루네컷에 이미지를 추가한다.
     */
    private void addImages(Day4Cut day4Cut, List<Day4CutReqDto.ImageReqDto> images) {
        for (int i = 0; i < images.size(); i++) {
            Day4CutReqDto.ImageReqDto imageDto = images.get(i);

            // 이미지가 1장인 경우 자동 썸네일 설정
            boolean isThumbnail = images.size() == 1 || Boolean.TRUE.equals(imageDto.isThumbnail());

            Day4CutImage image = Day4CutImage.builder()
                    .imageUrl(imageDto.url())
                    .isThumbnail(isThumbnail)
                    .build();

            day4Cut.addImage(image);
        }
    }
}
