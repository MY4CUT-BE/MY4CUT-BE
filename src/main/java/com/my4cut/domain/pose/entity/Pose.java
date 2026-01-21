package com.my4cut.domain.pose.entity;

import com.my4cut.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사진 촬영 포즈 정보를 저장하는 엔티티이다.
 * 포즈 제목, 이미지, 인원수 등 포즈 추천에 필요한 정보를 관리한다.
 */
@Entity
@Table(name = "poses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pose extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "people_count", nullable = false)
    private Integer peopleCount;

    @Builder
    public Pose(String title, String imageUrl, Integer peopleCount) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.peopleCount = peopleCount;
    }
}
