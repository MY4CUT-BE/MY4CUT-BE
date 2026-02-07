package com.my4cut.domain.day4cut.repository;

import com.my4cut.domain.day4cut.entity.Day4Cut;
import com.my4cut.domain.day4cut.entity.Day4CutImage;
import com.my4cut.domain.day4cut.enums.EmojiType;
import com.my4cut.domain.media.entity.MediaFile;
import com.my4cut.domain.media.enums.MediaType;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.enums.LoginType;
import com.my4cut.domain.user.enums.UserStatus;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class Day4CutRepositoryN1Test {

    @Autowired
    private EntityManager em;

    @Autowired
    private Day4CutRepository day4CutRepository;

    private User user;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.of(2026, 2, 7);

        // User 생성
        user = User.builder()
                .email("test@test.com")
                .nickname("tester")
                .loginType(LoginType.EMAIL)
                .friendCode("N1TEST99")
                .status(UserStatus.ACTIVE)
                .build();
        em.persist(user);

        // Day4Cut 생성
        Day4Cut day4Cut = Day4Cut.builder()
                .user(user)
                .date(testDate)
                .content("test content")
                .emojiType(EmojiType.HAPPY)
                .build();
        em.persist(day4Cut);

        // MediaFile 3개 생성 + Day4CutImage 3개 연결
        for (int i = 0; i < 3; i++) {
            MediaFile mediaFile = MediaFile.builder()
                    .uploader(user)
                    .mediaType(MediaType.PHOTO)
                    .fileUrl("https://example.com/image" + i + ".jpg")
                    .isFinal(true)
                    .build();
            em.persist(mediaFile);

            Day4CutImage image = Day4CutImage.builder()
                    .mediaFile(mediaFile)
                    .isThumbnail(i == 0)
                    .build();
            day4Cut.addImage(image);
        }

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("findByUserAndDate - fetch join으로 N+1 없이 1회 쿼리로 조회")
    void findByUserAndDate_noNPlusOne() {
        // Hibernate 통계 활성화
        Statistics stats = em.unwrap(Session.class).getSessionFactory().getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        // when
        Optional<Day4Cut> result = day4CutRepository.findByUserAndDate(user, testDate);

        // then - 엔티티 조회 확인
        assertThat(result).isPresent();
        Day4Cut day4Cut = result.get();

        // images와 mediaFile에 접근 (N+1 발생 지점)
        day4Cut.getImages().forEach(image -> {
            String url = image.getMediaFile().getFileUrl();
            assertThat(url).startsWith("https://example.com/image");
        });

        assertThat(day4Cut.getImages()).hasSize(3);

        // 쿼리 1회만 실행되었는지 검증
        long queryCount = stats.getPrepareStatementCount();
        System.out.println("=== 실행된 쿼리 수: " + queryCount + " ===");
        assertThat(queryCount)
                .as("fetch join으로 Day4Cut + images + mediaFile 을 1회 쿼리로 조회해야 합니다. 실행된 쿼리: %d", queryCount)
                .isEqualTo(1);
    }
}
