package com.my4cut.domain.pose.service;

import com.my4cut.domain.image.service.ImageStorageService;
import com.my4cut.domain.pose.dto.req.PoseCreateRequest;
import com.my4cut.domain.pose.dto.req.PoseUpdateRequest;
import com.my4cut.domain.pose.entity.Pose;
import com.my4cut.domain.pose.repository.PoseRepository;
import com.my4cut.global.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PoseAdminServiceTest {

    @Mock
    private PoseRepository poseRepository;
    @Mock
    private ImageStorageService imageStorageService;

    private PoseAdminService poseAdminService;

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
        PoseImageTransactionManager transactionManager =
                new PoseImageTransactionManager(imageStorageService);
        poseAdminService = new PoseAdminService(
                poseRepository,
                imageStorageService,
                new PoseImageValidator(),
                transactionManager
        );
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void createsPoseWithUploadedImageKey() {
        MockMultipartFile image = jpeg();
        given(imageStorageService.upload(image, "poses")).willReturn("poses/2026/07/new.jpg");

        poseAdminService.createPose(new PoseCreateRequest(" 새 포즈 ", 2), image);

        verify(poseRepository).saveAndFlush(any(Pose.class));
        verify(imageStorageService, never()).deleteIfExists(any());

        complete(TransactionSynchronization.STATUS_COMMITTED);
        verify(imageStorageService, never()).deleteIfExists(any());
    }

    @Test
    void deletesNewImageWhenCreateTransactionRollsBack() {
        MockMultipartFile image = jpeg();
        given(imageStorageService.upload(image, "poses")).willReturn("poses/new.jpg");
        given(poseRepository.saveAndFlush(any(Pose.class))).willThrow(new RuntimeException("db failure"));

        assertThatThrownBy(() ->
                poseAdminService.createPose(new PoseCreateRequest("새 포즈", 2), image)
        ).isInstanceOf(RuntimeException.class);

        complete(TransactionSynchronization.STATUS_ROLLED_BACK);
        verify(imageStorageService).deleteIfExists("poses/new.jpg");
    }

    @Test
    void updatesOnlyMetadataWithoutAccessingImageStorage() {
        Pose pose = Pose.builder()
                .title("기존")
                .peopleCount(2)
                .imageUrl("poses/old.jpg")
                .build();
        given(poseRepository.findById(1L)).willReturn(Optional.of(pose));

        poseAdminService.updatePose(1L, new PoseUpdateRequest("수정", 3), null);

        assertThat(pose.getTitle()).isEqualTo("수정");
        assertThat(pose.getPeopleCount()).isEqualTo(3);
        verify(imageStorageService, never()).upload(any(), any(String.class));
        verify(imageStorageService, never()).deleteIfExists(any());
    }

    @Test
    void replacesOldImageOnlyAfterCommit() {
        Pose pose = Pose.builder()
                .title("기존")
                .peopleCount(2)
                .imageUrl("poses/old.jpg")
                .build();
        MockMultipartFile image = jpeg();
        given(poseRepository.findById(1L)).willReturn(Optional.of(pose));
        given(imageStorageService.upload(image, "poses")).willReturn("poses/new.jpg");

        poseAdminService.updatePose(1L, null, image);

        assertThat(pose.getImageUrl()).isEqualTo("poses/new.jpg");
        verify(imageStorageService, never()).deleteIfExists(any());

        complete(TransactionSynchronization.STATUS_COMMITTED);
        verify(imageStorageService).deleteIfExists("poses/old.jpg");
    }

    @Test
    void deletesNewImageAndKeepsOldImageOnUpdateRollback() {
        Pose pose = Pose.builder()
                .title("기존")
                .peopleCount(2)
                .imageUrl("poses/old.jpg")
                .build();
        MockMultipartFile image = jpeg();
        given(poseRepository.findById(1L)).willReturn(Optional.of(pose));
        given(imageStorageService.upload(image, "poses")).willReturn("poses/new.jpg");
        given(poseRepository.saveAndFlush(pose)).willThrow(new RuntimeException("db failure"));

        assertThatThrownBy(() -> poseAdminService.updatePose(1L, null, image))
                .isInstanceOf(RuntimeException.class);

        complete(TransactionSynchronization.STATUS_ROLLED_BACK);
        verify(imageStorageService).deleteIfExists("poses/new.jpg");
        verify(imageStorageService, never()).deleteIfExists("poses/old.jpg");
    }

    @Test
    void deletesPoseImageOnlyAfterCommit() {
        Pose pose = Pose.builder()
                .title("삭제")
                .peopleCount(2)
                .imageUrl("poses/delete.jpg")
                .build();
        given(poseRepository.findById(1L)).willReturn(Optional.of(pose));

        poseAdminService.deletePose(1L);

        verify(poseRepository).delete(pose);
        verify(imageStorageService, never()).deleteIfExists(any());

        complete(TransactionSynchronization.STATUS_COMMITTED);
        verify(imageStorageService).deleteIfExists("poses/delete.jpg");
    }

    @Test
    void rejectsEmptyUpdate() {
        assertThatThrownBy(() -> poseAdminService.updatePose(1L, null, null))
                .isInstanceOf(BusinessException.class);
    }

    private void complete(int status) {
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization -> synchronization.afterCompletion(status));
    }

    private MockMultipartFile jpeg() {
        return new MockMultipartFile(
                "image",
                "pose.jpg",
                "image/jpeg",
                jpegBytes()
        );
    }

    private byte[] jpegBytes() {
        try {
            BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
