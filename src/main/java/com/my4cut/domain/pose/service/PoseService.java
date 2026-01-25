package com.my4cut.domain.pose.service;

import com.my4cut.domain.pose.dto.res.PoseResDto;
import com.my4cut.domain.pose.entity.Pose;
import com.my4cut.domain.pose.entity.PoseFavorite;
import com.my4cut.domain.pose.repository.PoseFavoriteRepository;
import com.my4cut.domain.pose.repository.PoseRepository;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.global.exception.BusinessException;
import com.my4cut.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PoseService {

    private final PoseRepository poseRepository;
    private final PoseFavoriteRepository poseFavoriteRepository;
    private final UserRepository userRepository;

    // 포즈 목록 조회
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
                .map(PoseResDto.PoseListResDto::of)
                .toList();
    }

    // 포즈 상세 조회
    @Transactional(readOnly = true)
    public PoseResDto.PoseDetailResDto getPoseDetail(Long poseId) {
        Pose pose = poseRepository.findById(poseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        return PoseResDto.PoseDetailResDto.of(pose);
    }

    // 포즈 즐겨찾기 등록
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

    // 포즈 즐겨찾기 해제
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
