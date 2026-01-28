package com.my4cut.domain.day4cut.entity;

import com.my4cut.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 하루네컷에 첨부된 이미지 정보를 저장하는 엔티티이다.
 * 하나의 하루네컷에 여러 이미지가 첨부될 수 있으며, 썸네일 여부를 관리한다.
 */
@Entity
@Table(name = "day4cut_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Day4CutImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "day4cut_id", nullable = false)
    private Day4Cut day4Cut;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "is_thumbnail", nullable = false)
    private Boolean isThumbnail;

    @Builder
    public Day4CutImage(String imageUrl, Boolean isThumbnail) {
        this.imageUrl = imageUrl;
        this.isThumbnail = isThumbnail;
    }

    void setDay4Cut(Day4Cut day4Cut) {
        this.day4Cut = day4Cut;
    }
}
