package com.my4cut.domain.album.domain;

import com.my4cut.domain.common.BaseEntity;
import com.my4cut.domain.media.entity.MediaFile;
import com.my4cut.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 앨범 정보를 저장하는 엔티티.
 * 사용자가 소유한 미디어 파일들을 그룹화하여 관리합니다.
 *
 * @author koohyunmo
 * @since 2026-01-27
 */
@Entity
@Table(name = "albums")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Album extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "album_name", nullable = false)
    private String albumName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @OneToMany(mappedBy = "album", cascade = CascadeType.ALL)
    @Builder.Default
    private List<MediaFile> mediaFiles = new ArrayList<>();

    /**
     * 앨범 이름을 수정합니다.
     * @param albumName 새로운 앨범 이름
     */
    public void updateAlbumName(String albumName) {
        this.albumName = albumName;
    }
}
