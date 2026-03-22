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
import java.util.Objects;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_object_id", nullable = false)
    private MediaObject mediaObject;

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
    public MediaFile(User uploader, Workspace workspace, Album album, MediaObject mediaObject, MediaType mediaType,
                     String fileUrl, LocalDate takenDate, String diary, Boolean isFinal) {
        this.uploader = uploader;
        this.workspace = workspace;
        this.album = album;
        // dedup 구조에서는 모든 MediaFile 이 반드시 하나의 MediaObject 를 가져야 한다.
        this.mediaObject = Objects.requireNonNull(mediaObject, "mediaObject must not be null");
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
