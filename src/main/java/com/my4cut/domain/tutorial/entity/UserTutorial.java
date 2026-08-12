package com.my4cut.domain.tutorial.entity;

import com.my4cut.domain.common.BaseEntity;
import com.my4cut.domain.tutorial.enums.TutorialType;
import com.my4cut.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_tutorials")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTutorial extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "home_completed", nullable = false)
    private boolean homeCompleted;

    @Column(name = "workspace_completed", nullable = false)
    private boolean workspaceCompleted;

    @Column(name = "photo_upload_completed", nullable = false)
    private boolean photoUploadCompleted;

    public UserTutorial(User user) {
        this.user = user;
    }

    public void complete(TutorialType tutorialType) {
        switch (tutorialType) {
            case HOME -> homeCompleted = true;
            case WORKSPACE -> workspaceCompleted = true;
            case PHOTO_UPLOAD -> photoUploadCompleted = true;
        }
    }
}
