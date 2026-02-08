package com.my4cut.domain.day4cut.service;

import com.my4cut.domain.day4cut.dto.req.Day4CutReqDto;
import com.my4cut.domain.day4cut.dto.res.Day4CutResDto;
import com.my4cut.domain.day4cut.entity.Day4Cut;
import com.my4cut.domain.day4cut.entity.Day4CutImage;
import com.my4cut.domain.day4cut.exception.Day4CutErrorCode;
import com.my4cut.domain.day4cut.exception.Day4CutException;
import com.my4cut.domain.day4cut.repository.Day4CutRepository;
import com.my4cut.domain.image.service.ImageStorageService;
import com.my4cut.domain.media.entity.MediaFile;
import com.my4cut.domain.media.repository.MediaFileRepository;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.enums.UserStatus;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.global.exception.BusinessException;
import com.my4cut.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 하루네컷 관련 비즈니스 로직을 처리하는 서비스 클래스.
 */
@Service
@RequiredArgsConstructor
public class Day4CutService {

    private final Day4CutRepository day4CutRepository;
    private final UserRepository userRepository;
    private final MediaFileRepository mediaFileRepository;
    private final ImageStorageService imageStorageService;


    /**
     * 하루네컷을 생성한다.
     */
    @Transactional
    public Day4CutResDto.CreateResDto createDay4Cut(Long userId, Day4CutReqDto.CreateReqDto reqDto) {
        User user = findUserById(userId);

        // 중복 검증
        if (day4CutRepository.existsByUserAndDate(user, reqDto.date())) {
            throw new Day4CutException(Day4CutErrorCode.DAY4CUT_ALREADY_EXISTS);
        }

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
        addImages(day4Cut, reqDto.images(), user);

        day4CutRepository.save(day4Cut);

        return Day4CutResDto.CreateResDto.of();
    }

    /**
     * 하루네컷을 조회한다.
     */
    @Transactional(readOnly = true)
    public Day4CutResDto.DetailResDto getDay4Cut(Long userId, LocalDate date) {
        User user = findUserById(userId);

        Day4Cut day4Cut = day4CutRepository.findByUserAndDate(user, date)
                .orElseThrow(() -> new Day4CutException(Day4CutErrorCode.DAY4CUT_NOT_FOUND));

        List<String> viewUrls = day4Cut.getImages().stream()
                .map(image -> imageStorageService
                        .generatePresignedGetUrl(image.getMediaFile().getFileUrl()))
                .toList();

        return Day4CutResDto.DetailResDto.of(day4Cut, viewUrls);
    }

    /**
     * 하루네컷을 수정한다.
     */
    @Transactional
    public Day4CutResDto.UpdateResDto updateDay4Cut(Long userId, Day4CutReqDto.UpdateReqDto reqDto) {
        User user = findUserById(userId);

        Day4Cut day4Cut = day4CutRepository.findByUserAndDate(user, reqDto.date())
                .orElseThrow(() -> new Day4CutException(Day4CutErrorCode.DAY4CUT_NOT_FOUND));

        // 이미지 검증
        validateImages(reqDto.images());

        // 내용 검증
        validateContent(reqDto.content());

        // 하루네컷 정보 수정
        day4Cut.update(reqDto.content(), reqDto.emojiType());

        // 기존 이미지 삭제 후 새 이미지 추가 (전체 교체)
        day4Cut.clearImages();
        addImages(day4Cut, reqDto.images(), user);

        return Day4CutResDto.UpdateResDto.of();
    }

    /**
     * 하루네컷을 삭제한다.
     */
    @Transactional
    public Day4CutResDto.DeleteResDto deleteDay4Cut(Long userId, LocalDate date) {
        User user = findUserById(userId);

        Day4Cut day4Cut = day4CutRepository.findByUserAndDate(user, date)
                .orElseThrow(() -> new Day4CutException(Day4CutErrorCode.DAY4CUT_NOT_FOUND));

        day4CutRepository.delete(day4Cut);

        return Day4CutResDto.DeleteResDto.of();
    }

    /**
     * 사용자를 조회한다.
     */
    private User findUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (user.getStatus() == UserStatus.DELETED) {
            throw new BusinessException(ErrorCode.USER_DELETED);
        }

        return user;
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
    private void addImages(Day4Cut day4Cut, List<Day4CutReqDto.ImageReqDto> images, User user) {
        for (int i = 0; i < images.size(); i++) {
            Day4CutReqDto.ImageReqDto imageDto = images.get(i);

            // MediaFile 조회
            MediaFile mediaFile = mediaFileRepository.findById(imageDto.mediaFileId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

            // 소유권 검증
            if (!mediaFile.getUploader().getId().equals(user.getId())) {
                throw new Day4CutException(Day4CutErrorCode.DAY4CUT_ACCESS_DENIED);
            }

            // 이미지가 1장인 경우 자동 썸네일 설정
            boolean isThumbnail = images.size() == 1 || Boolean.TRUE.equals(imageDto.isThumbnail());

            Day4CutImage image = Day4CutImage.builder()
                    .mediaFile(mediaFile)
                    .isThumbnail(isThumbnail)
                    .build();

            day4Cut.addImage(image);
        }
    }

    /**
     * 하루네컷이 존재하는 날짜 목록 조회
     */
    @Transactional(readOnly = true)
    public Day4CutResDto.CalendarResDto getCalendar(Long userId, int year, int month) {
        User user = findUserById(userId);

        List<Day4Cut> day4Cuts =
                day4CutRepository.findAllByUserAndYearMonth(user, year, month);

        List<Day4CutResDto.CalendarResDto.CalendarDayDto> days =
                day4Cuts.stream()
                        .map(day4Cut -> {
                            String thumbnailKey = day4Cut.getImages().stream()
                                    .filter(img -> Boolean.TRUE.equals(img.getIsThumbnail()))
                                    .findFirst()
                                    .map(img -> img.getMediaFile().getFileUrl())
                                    .orElse(null);

                            String thumbnailUrl = thumbnailKey == null
                                    ? null
                                    : imageStorageService.generatePresignedGetUrl(thumbnailKey);

                            return Day4CutResDto.CalendarResDto.CalendarDayDto.of(
                                    day4Cut.getDate().getDayOfMonth(),
                                    thumbnailUrl
                            );
                        })
                        .toList();

        return new Day4CutResDto.CalendarResDto(days);
    }
}
