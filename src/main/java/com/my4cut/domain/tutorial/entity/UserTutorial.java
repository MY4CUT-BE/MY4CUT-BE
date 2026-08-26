package com.my4cut.domain.tutorial.entity;

import com.my4cut.domain.common.BaseEntity;
import com.my4cut.domain.tutorial.enums.TutorialType;
import com.my4cut.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_tutorials",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_tutorials_user_type",
                columnNames = {"user_id", "tutorial_type"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTutorial extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "tutorial_type", nullable = false, length = 50)
    private TutorialType tutorialType;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    public UserTutorial(User user, TutorialType tutorialType) {
        this.user = user;
        this.tutorialType = tutorialType;
        this.completedAt = LocalDateTime.now();
    }
}
