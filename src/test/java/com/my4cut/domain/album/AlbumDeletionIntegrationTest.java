package com.my4cut.domain.album;

import com.my4cut.domain.album.domain.Album;
import com.my4cut.domain.day4cut.entity.Day4Cut;
import com.my4cut.domain.day4cut.entity.Day4CutImage;
import com.my4cut.domain.media.entity.MediaFile;
import com.my4cut.domain.media.entity.MediaObject;
import com.my4cut.domain.media.enums.MediaObjectStatus;
import com.my4cut.domain.media.enums.MediaType;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.enums.LoginType;
import com.my4cut.domain.user.enums.UserStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AlbumDeletionIntegrationTest {
    @Autowired EntityManager em;

    @Test
    void deletingAlbumPreservesMediaAndDiaryReference() {
        User user = User.builder().email("album@test.com").password("encoded")
                .nickname("album").friendCode("ALBUM1").loginType(LoginType.EMAIL)
                .status(UserStatus.ACTIVE).build();
        em.persist(user);
        Album album = Album.builder().albumName("album").owner(user).build();
        em.persist(album);
        MediaObject object = MediaObject.builder().owner(user).fileKey("test/photo.jpg")
                .status(MediaObjectStatus.ACTIVE).build();
        em.persist(object);
        MediaFile media = MediaFile.builder().uploader(user).album(album).mediaObject(object)
                .mediaType(MediaType.PHOTO).fileUrl("test/photo.jpg").isFinal(false).build();
        em.persist(media);
        Day4Cut diary = Day4Cut.builder().user(user).date(LocalDate.of(2026, 11, 12)).build();
        em.persist(diary);
        Day4CutImage image = Day4CutImage.builder().mediaFile(media).isThumbnail(true).build();
        ReflectionTestUtils.setField(image, "day4Cut", diary);
        em.persist(image);
        em.flush();
        Long albumId = album.getId(), mediaId = media.getId(), imageId = image.getId();
        em.clear();

        Album loaded = em.find(Album.class, albumId);
        loaded.getMediaFiles().forEach(file -> file.assignToAlbum(null));
        em.remove(loaded);
        em.flush();
        em.clear();

        assertThat(em.find(Album.class, albumId)).isNull();
        assertThat(em.find(MediaFile.class, mediaId)).isNotNull();
        assertThat(em.find(MediaFile.class, mediaId).getAlbum()).isNull();
        assertThat(em.find(Day4CutImage.class, imageId).getMediaFile().getId()).isEqualTo(mediaId);
        assertThat(em.find(MediaObject.class, object.getId())).isNotNull();
    }
}
