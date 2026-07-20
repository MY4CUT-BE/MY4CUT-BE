package com.my4cut.domain.pose.repository;

import com.my4cut.domain.pose.entity.Pose;
import com.my4cut.domain.pose.entity.PoseFavorite;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.enums.LoginType;
import com.my4cut.domain.user.enums.UserStatus;
import com.my4cut.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class PoseFavoriteConstraintTest {

    @Autowired
    private PoseRepository poseRepository;
    @Autowired
    private PoseFavoriteRepository poseFavoriteRepository;
    @Autowired
    private UserRepository userRepository;

    @Test
    void deletingPoseCascadesFavorites() {
        User user = userRepository.save(user("cascade@example.com", "CAS001"));
        Pose pose = poseRepository.save(pose("cascade"));
        poseFavoriteRepository.saveAndFlush(favorite(user, pose));

        poseRepository.delete(pose);
        poseRepository.flush();

        assertThat(poseFavoriteRepository.count()).isZero();
    }

    @Test
    void duplicateUserAndPoseFavoriteIsRejected() {
        User user = userRepository.save(user("unique@example.com", "UNQ001"));
        Pose pose = poseRepository.save(pose("unique"));
        poseFavoriteRepository.saveAndFlush(favorite(user, pose));

        assertThatThrownBy(() -> poseFavoriteRepository.saveAndFlush(favorite(user, pose)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private User user(String email, String friendCode) {
        return User.builder()
                .email(email)
                .password("password")
                .nickname("tester")
                .profileImageUrl("/images/default.png")
                .loginType(LoginType.EMAIL)
                .friendCode(friendCode)
                .status(UserStatus.ACTIVE)
                .build();
    }

    private Pose pose(String suffix) {
        return Pose.builder()
                .title("pose-" + suffix)
                .imageUrl("poses/" + suffix + ".jpg")
                .peopleCount(2)
                .build();
    }

    private PoseFavorite favorite(User user, Pose pose) {
        return PoseFavorite.builder()
                .user(user)
                .pose(pose)
                .build();
    }
}
