package com.my4cut.domain.pose.service;

import com.my4cut.domain.pose.dto.res.PoseResDto;
import com.my4cut.domain.pose.entity.Pose;
import com.my4cut.domain.pose.entity.PoseFavorite;
import com.my4cut.domain.pose.repository.PoseFavoriteRepository;
import com.my4cut.domain.pose.repository.PoseRepository;
import com.my4cut.domain.image.service.ImageStorageService;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.global.exception.BusinessException;
import com.my4cut.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 포즈 추천 관련 비즈니스 로직을 처리하는 서비스 클래스.
 * @author koohyunmo
 * @since 2026-02-08
 */
@Service
@RequiredArgsConstructor
public class PoseService {

    private final PoseRepository poseRepository;
    private final PoseFavoriteRepository poseFavoriteRepository;
    private final UserRepository userRepository;
    private final ImageStorageService imageStorageService;

    /**
     * 포즈 목록을 조회합니다.
     * @param sort 정렬 기준 (title, peopleCount 등)
     * @param peopleCount 필터링할 인원수 (선택)
     * @return 포즈 목록 DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<PoseResDto.PoseListResDto> getPoseList(String sort, Integer peopleCount) {
        List<Pose> poses;

        Sort sortOrder = Sort.by(Sort.Direction.DESC, "createdAt");
        if ("title".equals(sort)) {
            sortOrder = Sort.by(Sort.Direction.ASC, "title");
        } else if ("peopleCount".equals(sort)) {
            sortOrder = Sort.by(Sort.Direction.ASC, "peopleCount");
        }

        if (peopleCount != null) {
            poses = poseRepository.findAllByPeopleCount(peopleCount);
        } else {
            poses = poseRepository.findAll(sortOrder);
        }

        return poses.stream()
                .map(pose -> PoseResDto.PoseListResDto.of(
                        pose,
                        imageStorageService.generatePresignedGetUrl(pose.getImageUrl())
                ))
                .toList();
    }

    /**
     * 특정 포즈의 상세 정보를 조회합니다.
     * @param poseId 조회할 포즈 ID
     * @return 포즈 상세 정보 DTO
     */
    @Transactional(readOnly = true)
    public PoseResDto.PoseDetailResDto getPoseDetail(Long poseId) {
        Pose pose = poseRepository.findById(poseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        return PoseResDto.PoseDetailResDto.of(
                pose,
                imageStorageService.generatePresignedGetUrl(pose.getImageUrl())
        );
    }

    /**
     * 포즈를 즐겨찾기에 등록합니다.
     * @param userId 유저 ID
     * @param poseId 포즈 ID
     * @return 즐겨찾기 결과 DTO
     */
    @Transactional
    public PoseResDto.BookmarkResDto addBookmark(Long userId, Long poseId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        Pose pose = poseRepository.findById(poseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        // 이미 즐겨찾기한 경우
        if (poseFavoriteRepository.existsByUserAndPose(user, pose)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        PoseFavorite poseFavorite = PoseFavorite.builder()
                .user(user)
                .pose(pose)
                .build();

        poseFavoriteRepository.save(poseFavorite);

        return PoseResDto.BookmarkResDto.of(true);
    }

    /**
     * 포즈 즐겨찾기를 해제합니다.
     * @param userId 유저 ID
     * @param poseId 포즈 ID
     * @return 즐겨찾기 해제 결과 DTO
     */
    @Transactional
    public PoseResDto.BookmarkResDto removeBookmark(Long userId, Long poseId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        Pose pose = poseRepository.findById(poseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        // 즐겨찾기하지 않은 경우
        if (!poseFavoriteRepository.existsByUserAndPose(user, pose)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        poseFavoriteRepository.deleteByUserAndPose(user, pose);

        return PoseResDto.BookmarkResDto.of(true);
    }
}
