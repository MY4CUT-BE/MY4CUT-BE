package com.my4cut.domain.media.entity;

import com.my4cut.domain.common.BaseEntity;
import com.my4cut.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 미디어 파일에 대한 댓글 정보를 저장하는 엔티티이다.
 * 사용자가 미디어에 남긴 댓글 내용을 관리한다.
 */
@Entity
@Table(name = "media_comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MediaComment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id", nullable = false)
    private MediaFile mediaFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Creates a new MediaComment that associates a user with a comment on a media file.
     *
     * @param mediaFile the media file being commented on; must not be null
     * @param user the author of the comment; must not be null
     * @param content the comment text to store (persisted as TEXT); must not be null
     */
    @Builder
    public MediaComment(MediaFile mediaFile, User user, String content) {
        this.mediaFile = mediaFile;
        this.user = user;
        this.content = content;
    }
}