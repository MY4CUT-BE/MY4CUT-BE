package com.my4cut.domain.media.entity;

import com.my4cut.domain.common.BaseEntity;
import com.my4cut.domain.media.enums.MediaObjectStatus;
import com.my4cut.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "media_objects",
        uniqueConstraints = {
                // 같은 사용자 범위에서만 동일 파일 dedup 을 허용한다.
                @UniqueConstraint(name = "uk_media_object_owner_hash_size", columnNames = {"owner_id", "sha256", "file_size"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MediaObject extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(length = 64)
    private String sha256;

    @Column(name = "file_key", nullable = false)
    private String fileKey;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "content_type")
    private String contentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MediaObjectStatus status;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public MediaObject(User owner, String sha256, String fileKey, Long fileSize, String contentType, MediaObjectStatus status) {
        this.owner = owner;
        this.sha256 = sha256;
        this.fileKey = fileKey;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.status = status;
    }

    public void activate(String sha256, String fileKey, Long fileSize, String contentType) {
        // 재사용 가능한 객체를 다시 ACTIVE 상태로 되돌릴 때 메타데이터를 함께 복구한다.
        this.sha256 = sha256;
        this.fileKey = fileKey;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.status = MediaObjectStatus.ACTIVE;
        this.deletedAt = null;
    }

    public void markPendingDelete() {
        // 실제 파일 삭제 전에 DB 에 삭제 대기 상태를 먼저 남긴다.
        this.status = MediaObjectStatus.PENDING_DELETE;
    }

    public void markDeleted() {
        // 물리 파일 삭제까지 끝난 뒤 최종 삭제 상태를 확정한다.
        this.status = MediaObjectStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }
}
