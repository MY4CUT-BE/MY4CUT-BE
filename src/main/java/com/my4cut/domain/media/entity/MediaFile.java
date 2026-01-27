package com.my4cut.domain.media.entity;

import com.my4cut.domain.album.domain.Album;
import com.my4cut.domain.common.BaseEntity;
import com.my4cut.domain.media.enums.MediaType;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.workspace.entity.Workspace;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 미디어 파일 정보를 저장하는 엔티티이다.
 * 워크스페이스에 업로드된 사진이나 동영상과 관련 다이어리 내용을 관리한다.
 */
@Entity
@Table(name = "media_files")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MediaFile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id")
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id")
    private Album album;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    private MediaType mediaType;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "taken_date")
    private LocalDate takenDate;

    @Column(columnDefinition = "TEXT")
    private String diary;

    @Column(name = "is_final", nullable = false)
    private Boolean isFinal;

    @Builder
    public MediaFile(User uploader, Workspace workspace, Album album, MediaType mediaType,
                     String fileUrl, LocalDate takenDate, String diary, Boolean isFinal) {
        this.uploader = uploader;
        this.workspace = workspace;
        this.album = album;
        this.mediaType = mediaType;
        this.fileUrl = fileUrl;
        this.takenDate = takenDate;
        this.diary = diary;
        this.isFinal = isFinal;
    }

    public void assignToWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    public void assignToAlbum(Album album) {
        this.album = album;
    }
}
