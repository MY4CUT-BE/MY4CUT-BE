package com.my4cut.domain.album.repository;

import com.my4cut.domain.album.domain.Album;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 앨범 엔티티에 대한 데이터 관리 리포지토리.
 * @author koohyunmo
 * @since 2026-01-27
 */
@Repository
public interface AlbumRepository extends JpaRepository<Album, Long> {
    /**
     * 소유자 ID로 앨범 목록을 조회합니다.
     * @param ownerId 소유자의 고유 ID
     * @return 소유자의 앨범 리스트
     */
    List<Album> findAllByOwnerId(Long ownerId);
}
