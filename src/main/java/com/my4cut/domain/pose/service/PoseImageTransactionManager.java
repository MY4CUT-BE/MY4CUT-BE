package com.my4cut.domain.pose.service;

import com.my4cut.domain.image.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class PoseImageTransactionManager {

    private final ImageStorageService imageStorageService;

    public void deleteNewImageOnRollback(String newImagePath) {
        register(null, newImagePath);
    }

    public void replaceImageAfterCompletion(String oldImagePath, String newImagePath) {
        register(oldImagePath, newImagePath);
    }

    public void deleteImageAfterCommit(String oldImagePath) {
        register(oldImagePath, null);
    }

    private void register(String deleteOnCommit, String deleteOnRollback) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            if (deleteOnRollback != null) {
                deleteSafely(deleteOnRollback);
            }
            throw new IllegalStateException("Pose image lifecycle must run inside a transaction.");
        }

        try {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == TransactionSynchronization.STATUS_COMMITTED) {
                        deleteSafely(deleteOnCommit);
                    } else {
                        deleteSafely(deleteOnRollback);
                    }
                }
            });
        } catch (RuntimeException exception) {
            if (deleteOnRollback != null) {
                deleteSafely(deleteOnRollback);
            }
            throw exception;
        }
    }

    private void deleteSafely(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return;
        }

        try {
            if (!imageStorageService.deleteIfExists(imagePath)) {
                log.warn("Failed to clean up pose image: {}", imagePath);
            }
        } catch (RuntimeException exception) {
            log.warn("Failed to clean up pose image: {}", imagePath, exception);
        }
    }
}
