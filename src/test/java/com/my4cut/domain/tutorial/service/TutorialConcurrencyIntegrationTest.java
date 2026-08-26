package com.my4cut.domain.tutorial.service;

import com.my4cut.domain.tutorial.dto.TutorialStatusResponseDto;
import com.my4cut.domain.tutorial.enums.TutorialType;
import com.my4cut.domain.tutorial.repository.UserTutorialRepository;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.enums.LoginType;
import com.my4cut.domain.user.enums.UserStatus;
import com.my4cut.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TutorialService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TutorialConcurrencyIntegrationTest {

    @Autowired
    private TutorialService tutorialService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserTutorialRepository userTutorialRepository;

    @AfterEach
    void cleanUp() {
        userTutorialRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void concurrentStatusRequests_doNotCreateProgressRows() throws Exception {
        Long userId = createUser().getId();

        runConcurrently(
                () -> tutorialService.getStatus(userId),
                () -> tutorialService.getStatus(userId)
        );

        assertThat(userTutorialRepository.findAllByUserId(userId)).isEmpty();
    }

    @Test
    void concurrentDifferentCompletions_preserveBothFlags() throws Exception {
        Long userId = createUser().getId();
        runConcurrently(
                () -> tutorialService.complete(userId, TutorialType.HOME),
                () -> tutorialService.complete(userId, TutorialType.UPLOAD_DATE)
        );

        TutorialStatusResponseDto status = tutorialService.getStatus(userId);
        assertThat(completed(status, TutorialType.HOME)).isTrue();
        assertThat(completed(status, TutorialType.UPLOAD_DATE)).isTrue();
        assertThat(completed(status, TutorialType.UPLOAD_CONTENT)).isFalse();
        assertThat(userTutorialRepository.findAllByUserId(userId)).hasSize(2);
    }

    @Test
    void concurrentSameCompletion_createsOnlyOneProgressRow() throws Exception {
        Long userId = createUser().getId();

        runConcurrently(
                () -> tutorialService.complete(userId, TutorialType.HOME),
                () -> tutorialService.complete(userId, TutorialType.HOME)
        );

        assertThat(userTutorialRepository.findAllByUserId(userId)).hasSize(1);
    }

    private boolean completed(TutorialStatusResponseDto response, TutorialType type) {
        return response.tutorials().stream()
                .filter(status -> status.type() == type)
                .findFirst()
                .orElseThrow()
                .completed();
    }

    private User createUser() {
        String uniqueValue = UUID.randomUUID().toString();
        return userRepository.saveAndFlush(User.builder()
                .email(uniqueValue + "@example.com")
                .password("encoded-password")
                .nickname("user")
                .loginType(LoginType.EMAIL)
                .friendCode(uniqueValue.substring(0, 6).toUpperCase())
                .status(UserStatus.ACTIVE)
                .build());
    }

    private void runConcurrently(Runnable firstTask, Runnable secondTask) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<?> first = executor.submit(() -> runAfterSignal(firstTask, ready, start));
            Future<?> second = executor.submit(() -> runAfterSignal(secondTask, ready, start));

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    private void runAfterSignal(
            Runnable task,
            CountDownLatch ready,
            CountDownLatch start
    ) {
        ready.countDown();
        try {
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent test did not start in time");
            }
            task.run();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
