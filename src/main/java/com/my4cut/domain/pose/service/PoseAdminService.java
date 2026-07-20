package com.my4cut.domain.pose.service;

import com.my4cut.domain.image.service.ImageStorageService;
import com.my4cut.domain.pose.dto.req.PoseCreateRequest;
import com.my4cut.domain.pose.dto.req.PoseUpdateRequest;
import com.my4cut.domain.pose.entity.Pose;
import com.my4cut.domain.pose.repository.PoseRepository;
import com.my4cut.global.exception.BusinessException;
import com.my4cut.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class PoseAdminService {

    private final PoseRepository poseRepository;
    private final ImageStorageService imageStorageService;
    private final PoseImageValidator poseImageValidator;
    private final PoseImageTransactionManager poseImageTransactionManager;

    @Transactional
    public void createPose(PoseCreateRequest request, MultipartFile image) {
        validateTitle(request.title());
        validatePeopleCount(request.peopleCount());
        poseImageValidator.validate(image);

        String imageKey = imageStorageService.upload(image, "poses");
        poseImageTransactionManager.deleteNewImageOnRollback(imageKey);

        Pose pose = Pose.builder()
                .title(request.title().trim())
                .peopleCount(request.peopleCount())
                .imageUrl(imageKey)
                .build();
        poseRepository.saveAndFlush(pose);
    }

    @Transactional
    public void updatePose(Long poseId, PoseUpdateRequest request, MultipartFile image) {
        boolean hasMetadataChange = request != null
                && (request.title() != null || request.peopleCount() != null);
        boolean hasImageChange = image != null;
        if (!hasMetadataChange && !hasImageChange) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        Pose pose = findPose(poseId);

        String title = request == null ? null : request.title();
        Integer peopleCount = request == null ? null : request.peopleCount();
        if (title != null) {
            validateTitle(title);
            title = title.trim();
        }
        if (peopleCount != null) {
            validatePeopleCount(peopleCount);
        }

        pose.update(title, peopleCount);

        if (hasImageChange) {
            poseImageValidator.validate(image);
            String oldImagePath = pose.getImageUrl();
            String newImageKey = imageStorageService.upload(image, "poses");
            poseImageTransactionManager.replaceImageAfterCompletion(oldImagePath, newImageKey);
            pose.updateImage(newImageKey);
        }

        poseRepository.saveAndFlush(pose);
    }

    @Transactional
    public void deletePose(Long poseId) {
        Pose pose = findPose(poseId);
        String imagePath = pose.getImageUrl();

        poseRepository.delete(pose);
        poseImageTransactionManager.deleteImageAfterCommit(imagePath);
        poseRepository.flush();
    }

    private Pose findPose(Long poseId) {
        return poseRepository.findById(poseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    private void validateTitle(String title) {
        if (title == null || title.isBlank() || title.trim().length() > 255) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
    }

    private void validatePeopleCount(Integer peopleCount) {
        if (peopleCount == null || peopleCount < 1 || peopleCount > 10) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
    }
}
