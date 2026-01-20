package com.my4cut.domain.media.entity;

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
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

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

    /**
     * Create a MediaFile with the given uploader, workspace, media type, storage location, taken date, diary content, and finalization flag.
     *
     * @param uploader   the user who uploaded the media
     * @param workspace  the workspace where the media is stored
     * @param mediaType  the media's type
     * @param fileUrl    the URL or storage path of the media file
     * @param takenDate  the date the media was taken, or {@code null} if unknown
     * @param diary      notes or diary content associated with the media, may be {@code null}
     * @param isFinal    {@code true} if the media is finalized, {@code false} otherwise
     */
    @Builder
    public MediaFile(User uploader, Workspace workspace, MediaType mediaType,
                     String fileUrl, LocalDate takenDate, String diary, Boolean isFinal) {
        this.uploader = uploader;
        this.workspace = workspace;
        this.mediaType = mediaType;
        this.fileUrl = fileUrl;
        this.takenDate = takenDate;
        this.diary = diary;
        this.isFinal = isFinal;
    }
}