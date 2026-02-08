package com.my4cut.domain.album.service;

import com.my4cut.domain.album.domain.Album;
import com.my4cut.domain.album.dto.AlbumRequestDto;
import com.my4cut.domain.album.dto.AlbumResponseDto;
import com.my4cut.domain.album.exception.AlbumErrorCode;
import com.my4cut.domain.album.exception.AlbumException;
import com.my4cut.domain.album.repository.AlbumRepository;
import com.my4cut.domain.image.service.ImageStorageService;
import com.my4cut.domain.media.entity.MediaFile;
import com.my4cut.domain.media.repository.MediaFileRepository;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.domain.workspace.dto.WorkspacePhotoResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 앨범 관련 비즈니스 로직을 처리하는 서비스 클래스.
 * @author koohyunmo
 * @since 2026-01-27
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlbumService {

    private final AlbumRepository albumRepository;
    private final MediaFileRepository mediaFileRepository;
    private final UserRepository userRepository;
    private final ImageStorageService imageStorageService;

    /**
     * 새로운 앨범을 생성합니다.
     * @param requestDto 앨범 생성 정보 DTO
     * @param userId 유저 ID
     * @return 생성된 앨범 정보 DTO
     */
    @Transactional
    public AlbumResponseDto.Info createAlbum(AlbumRequestDto.CreateOrUpdate requestDto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Album album = Album.builder()
                .albumName(requestDto.name())
                .owner(user)
                .build();

        Album savedAlbum = albumRepository.save(album);
        return mapToInfo(savedAlbum);
    }

    /**
     * 사용자의 모든 앨범 목록을 조회합니다.
     * @param userId 유저 ID
     * @return 앨범 정보 DTO 리스트
     */
    public List<AlbumResponseDto.Info> getMyAlbums(Long userId) {
        return albumRepository.findAllByOwnerId(userId).stream()
                .map(this::mapToInfo)
                .collect(Collectors.toList());
    }

    /**
     * 특정 앨범의 상세 정보를 조회합니다.
     * @param albumId 앨범 ID
     * @param userId 유저 ID
     * @return 앨범 상세 정보 DTO
     */
    public AlbumResponseDto.Detail getAlbumDetail(Long albumId, Long userId) {
        Album album = validateAlbumOwner(albumId, userId);

        List<WorkspacePhotoResponseDto> mediaList = album.getMediaFiles().stream()
                .map(this::mapToMediaDto)
                .collect(Collectors.toList());

        return new AlbumResponseDto.Detail(
                album.getId(),
                album.getAlbumName(),
                mediaList,
                album.getCreatedAt()
        );
    }

    /**
     * 앨범 이름을 수정합니다.
     * @param albumId 앨범 ID
     * @param requestDto 수정할 앨범 정보 DTO
     * @param userId 유저 ID
     * @return 수정된 앨범 정보 DTO
     */
    @Transactional
    public AlbumResponseDto.Info updateAlbumName(Long albumId, AlbumRequestDto.CreateOrUpdate requestDto, Long userId) {
        Album album = validateAlbumOwner(albumId, userId);
        album.updateAlbumName(requestDto.name());
        return mapToInfo(album);
    }

    /**
     * 앨범을 삭제합니다.
     * @param albumId 앨범 ID
     * @param userId 유저 ID
     */
    @Transactional
    public void deleteAlbum(Long albumId, Long userId) {
        Album album = validateAlbumOwner(albumId, userId);
        
        // 연관된 미디어 파일들의 앨범 참조 해제
        for (MediaFile mediaFile : album.getMediaFiles()) {
            mediaFile.assignToAlbum(null);
        }
        
        albumRepository.delete(album);
    }

    /**
     * 앨범에 미디어를 추가합니다.
     * @param albumId 앨범 ID
     * @param requestDto 추가할 미디어 ID 리스트를 담은 DTO
     * @param userId 유저 ID
     * @return 업데이트된 앨범 상세 정보 DTO
     */
    @Transactional
    public AlbumResponseDto.Detail addMediaToAlbum(Long albumId, AlbumRequestDto.UpdateMedia requestDto, Long userId) {
        Album album = validateAlbumOwner(albumId, userId);

        List<MediaFile> mediaFiles = mediaFileRepository.findAllById(requestDto.mediaIds());
        
        for (MediaFile mediaFile : mediaFiles) {
            // 소유권 확인 (본인 미디어만 추가 가능)
            if (!mediaFile.getUploader().getId().equals(userId)) {
                throw new AlbumException(AlbumErrorCode.NOT_ALBUM_OWNER); // 혹은 적절한 에러
            }
            // 앨범 할당 (1:N 관계이므로 자동으로 이전 앨범에서 빠짐)
            mediaFile.assignToAlbum(album); 
        }

        return getAlbumDetail(albumId, userId);
    }

    /**
     * 앨범에서 미디어를 제외합니다.
     * @param albumId 앨범 ID
     * @param requestDto 제외할 미디어 ID 리스트를 담은 DTO
     * @param userId 유저 ID
     * @return 업데이트된 앨범 상세 정보 DTO
     */
    @Transactional
    public AlbumResponseDto.Detail removeMediaFromAlbum(Long albumId, AlbumRequestDto.UpdateMedia requestDto, Long userId) {
        validateAlbumOwner(albumId, userId);

        List<MediaFile> mediaFiles = mediaFileRepository.findAllById(requestDto.mediaIds());
        
        for (MediaFile mediaFile : mediaFiles) {
            if (mediaFile.getAlbum() != null && mediaFile.getAlbum().getId().equals(albumId)) {
                mediaFile.assignToAlbum(null);
            }
        }

        return getAlbumDetail(albumId, userId);
    }

    private Album validateAlbumOwner(Long albumId, Long userId) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new AlbumException(AlbumErrorCode.ALBUM_NOT_FOUND));

        if (!album.getOwner().getId().equals(userId)) {
            throw new AlbumException(AlbumErrorCode.NOT_ALBUM_OWNER);
        }
        return album;
    }

    private AlbumResponseDto.Info mapToInfo(Album album) {
        String coverKey = album.getMediaFiles().isEmpty() ? null : album.getMediaFiles().get(0).getFileUrl();
        String coverUrl = imageStorageService.generatePresignedGetUrl(coverKey);
        return new AlbumResponseDto.Info(
                album.getId(),
                album.getAlbumName(),
                album.getMediaFiles().size(),
                coverKey,
                coverUrl,
                album.getCreatedAt()
        );
    }

    private WorkspacePhotoResponseDto mapToMediaDto(MediaFile file) {
        return new WorkspacePhotoResponseDto(
                file.getId(),
                file.getFileUrl(),
                imageStorageService.generatePresignedGetUrl(file.getFileUrl()),
                file.getMediaType(),
                file.getTakenDate(),
                file.getIsFinal(),
                file.getCreatedAt(),
                file.getUploader().getNickname()
        );
    }
}
