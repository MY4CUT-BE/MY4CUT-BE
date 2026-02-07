package com.my4cut.domain.day4cut.entity;

import com.my4cut.domain.common.BaseEntity;
import com.my4cut.domain.day4cut.enums.EmojiType;
import com.my4cut.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 하루네컷 정보를 저장하는 엔티티이다.
 * 사용자가 작성한 날짜, 내용, 이모티콘 정보를 관리한다.
 */
@Entity
@Table(name = "day4cut", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "date"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Day4Cut extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "emoji_type")
    private EmojiType emojiType;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "day4Cut", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Day4CutImage> images = new ArrayList<>();

    @Builder
    public Day4Cut(User user, LocalDate date, String content, EmojiType emojiType) {
        this.user = user;
        this.date = date;
        this.content = content;
        this.emojiType = emojiType;
    }

    public void update(String content, EmojiType emojiType) {
        this.content = content;
        this.emojiType = emojiType;
        this.updatedAt = LocalDateTime.now();
    }

    public void addImage(Day4CutImage image) {
        this.images.add(image);
        image.setDay4Cut(this);
    }

    public void clearImages() {
        this.images.clear();
    }
}
