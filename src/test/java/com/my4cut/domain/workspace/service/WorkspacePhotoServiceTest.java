package com.my4cut.domain.workspace.service;

import com.my4cut.domain.image.service.ImageStorageService;
import com.my4cut.domain.image.service.ProfileImageUrlService;
import com.my4cut.domain.media.entity.MediaComment;
import com.my4cut.domain.media.entity.MediaFile;
import com.my4cut.domain.media.entity.MediaObject;
import com.my4cut.domain.media.enums.MediaObjectStatus;
import com.my4cut.domain.media.enums.MediaType;
import com.my4cut.domain.media.repository.MediaCommentRepository;
import com.my4cut.domain.media.repository.MediaFileRepository;
import com.my4cut.domain.media.service.MediaFileLifecycleService;
import com.my4cut.domain.notification.service.NotificationService;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.domain.workspace.dto.WorkspacePhotoCommentRequestDto;
import com.my4cut.domain.workspace.dto.WorkspacePhotoResponseDto;
import com.my4cut.domain.workspace.dto.WorkspacePhotoUploadRequestDto;
import com.my4cut.domain.workspace.entity.Workspace;
import com.my4cut.domain.workspace.entity.WorkspaceMember;
import com.my4cut.domain.workspace.exception.WorkspaceErrorCode;
import com.my4cut.domain.workspace.exception.WorkspaceException;
import com.my4cut.domain.workspace.repository.WorkspaceMemberRepository;
import com.my4cut.domain.workspace.repository.WorkspaceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspacePhotoServiceTest {

    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private MediaFileRepository mediaFileRepository;
    @Mock private MediaCommentRepository mediaCommentRepository;
    @Mock private UserRepository userRepository;
    @Mock private ImageStorageService imageStorageService;
    @Mock private MediaFileLifecycleService mediaFileLifecycleService;
    @Mock private ProfileImageUrlService profileImageUrlService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private WorkspacePhotoService workspacePhotoService;

    @Test
    @DisplayName("사진 업로드 성공: 선택한 미디어들을 워크스페이스에 할당한다")
    void uploadPhotos_Success() {
        // Arrange
        Long workspaceId = 1L;
        Long userId = 1L;
        Long mediaId = 10L;
        User user = createUser(userId, "유저");
        Workspace workspace = createWorkspace(workspaceId, "워크스페이스", user);
        MediaFile mediaFile = createMediaFile(user, null);
        ReflectionTestUtils.setField(mediaFile, "id", mediaId);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)).willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.findByWorkspaceAndUser(workspace, user)).willReturn(Optional.of(WorkspaceMember.builder().build()));
        given(mediaFileRepository.findById(mediaId)).willReturn(Optional.of(mediaFile));
        given(imageStorageService.generatePresignedGetUrl("url")).willReturn("presigned-url");

        WorkspacePhotoUploadRequestDto requestDto = new WorkspacePhotoUploadRequestDto(List.of(mediaId));

        // Act
        List<WorkspacePhotoResponseDto> result = workspacePhotoService.uploadPhotos(workspaceId, requestDto, userId);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(mediaFile.getWorkspace()).isEqualTo(workspace);
    }

    @Test
    @DisplayName("사진 업로드 성공: 업로더를 제외한 워크스페이스 멤버에게 알림을 발송한다")
    void uploadPhotos_NotifiesMembersExceptUploader() {
        Long workspaceId = 1L;
        Long uploaderId = 1L;
        Long mediaId = 10L;
        User uploader = createUser(uploaderId, "업로더");
        User member = createUser(2L, "멤버");
        Workspace workspace = createWorkspace(workspaceId, "워크스페이스", uploader);
        MediaFile mediaFile = createMediaFile(uploader, null);
        ReflectionTestUtils.setField(mediaFile, "id", mediaId);

        given(userRepository.findById(uploaderId)).willReturn(Optional.of(uploader));
        given(workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)).willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.findByWorkspaceAndUser(workspace, uploader))
                .willReturn(Optional.of(WorkspaceMember.builder().workspace(workspace).user(uploader).build()));
        given(workspaceMemberRepository.findAllByWorkspaceId(workspaceId)).willReturn(List.of(
                WorkspaceMember.builder().workspace(workspace).user(uploader).build(),
                WorkspaceMember.builder().workspace(workspace).user(member).build()
        ));
        given(mediaFileRepository.findById(mediaId)).willReturn(Optional.of(mediaFile));

        workspacePhotoService.uploadPhotos(
                workspaceId,
                new WorkspacePhotoUploadRequestDto(List.of(mediaId)),
                uploaderId
        );

        verify(notificationService).sendMediaUploadedNotification(member, uploader, workspaceId, mediaId);
        verify(notificationService, never())
                .sendMediaUploadedNotification(uploader, uploader, workspaceId, mediaId);
    }

    @Test
    @DisplayName("사진 업로드 실패: 만료된 워크스페이스인 경우 예외 발생")
    void uploadPhotos_Fail_Expired() {
        // Arrange
        Long workspaceId = 1L;
        Long userId = 1L;
        User user = createUser(userId, "멤버");
        Workspace workspace = createWorkspace(workspaceId, "만료됨", user);
        workspace.setExpiresAt(LocalDateTime.now().minusDays(1));
        WorkspacePhotoUploadRequestDto requestDto = new WorkspacePhotoUploadRequestDto(List.of(100L));

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)).willReturn(Optional.of(workspace));

        // Act & Assert
        assertThatThrownBy(() -> workspacePhotoService.uploadPhotos(workspaceId, requestDto, userId))
                .isInstanceOf(WorkspaceException.class)
                .hasFieldOrPropertyWithValue("errorCode", WorkspaceErrorCode.WORKSPACE_EXPIRED);
    }

    @Test
    @DisplayName("사진 목록 조회 성공: 사진별 댓글 수를 함께 반환한다")
    void getPhotos_IncludesCommentCount() {
        // Arrange
        Long workspaceId = 1L;
        Long photoId = 10L;
        Long userId = 1L;
        User user = createUser(userId, "유저");
        Workspace workspace = createWorkspace(workspaceId, "워크스페이스", user);
        MediaFile photo = createMediaFile(user, workspace);
        ReflectionTestUtils.setField(photo, "id", photoId);
        MediaCommentRepository.MediaCommentCount commentCount =
                mock(MediaCommentRepository.MediaCommentCount.class);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId))
                .willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.findByWorkspaceAndUser(workspace, user))
                .willReturn(Optional.of(WorkspaceMember.builder().build()));
        given(mediaFileRepository.findAllByWorkspaceIdAndMediaType(
                eq(workspaceId), eq(MediaType.PHOTO), any(Sort.class)))
                .willReturn(List.of(photo));
        given(mediaCommentRepository.countByMediaFileIds(List.of(photoId)))
                .willReturn(List.of(commentCount));
        given(commentCount.getMediaId()).willReturn(photoId);
        given(commentCount.getCommentCount()).willReturn(3L);

        // Act
        List<WorkspacePhotoResponseDto> result =
                workspacePhotoService.getPhotos(workspaceId, "latest", userId);

        // Assert
        assertThat(result).singleElement()
                .extracting(WorkspacePhotoResponseDto::commentCount)
                .isEqualTo(3L);
        verify(mediaCommentRepository).countByMediaFileIds(List.of(photoId));
    }

    @Test
    @DisplayName("사진 삭제 성공: 본인이 올린 사진을 삭제한다")
    void deletePhoto_Success() {
        // Arrange
        Long workspaceId = 1L;
        Long photoId = 10L;
        Long userId = 1L;
        User user = createUser(userId, "유저");
        Workspace workspace = createWorkspace(workspaceId, "워크스페이스", user);
        MediaFile photo = createMediaFile(user, workspace);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)).willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.findByWorkspaceAndUser(workspace, user)).willReturn(Optional.of(WorkspaceMember.builder().build()));
        given(mediaFileRepository.findById(photoId)).willReturn(Optional.of(photo));

        // Act
        workspacePhotoService.deletePhoto(workspaceId, photoId, userId);

        // Assert
        verify(mediaFileLifecycleService).deleteMediaFile(photo);
    }

    @Test
    @DisplayName("최종 사진 선택 성공: 기존 최종 사진을 해제하고 선택한 사진을 최종 사진으로 설정한다")
    void selectFinalPhoto_Success() {
        // Arrange
        Long workspaceId = 1L;
        Long photoId = 10L;
        Long userId = 1L;
        User user = createUser(userId, "사용자");
        Workspace workspace = createWorkspace(workspaceId, "워크스페이스", user);
        MediaFile photo = createMediaFile(user, workspace);
        ReflectionTestUtils.setField(photo, "id", photoId);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(workspaceRepository.findByIdAndDeletedAtIsNullForUpdate(workspaceId)).willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.findByWorkspaceAndUser(workspace, user)).willReturn(Optional.of(WorkspaceMember.builder().build()));
        given(mediaFileRepository.findById(photoId)).willReturn(Optional.of(photo));

        // Act
        workspacePhotoService.selectFinalPhoto(workspaceId, photoId, userId);

        // Assert
        verify(mediaFileRepository).clearFinalPhotosExcept(workspaceId, MediaType.PHOTO, photoId);
        assertThat(photo.getIsFinal()).isTrue();
    }

    @Test
    @DisplayName("최종 사진 선택 해제 성공: 선택된 사진을 최종 사진에서 해제한다")
    void deselectFinalPhoto_Success() {
        // Arrange
        Long workspaceId = 1L;
        Long photoId = 10L;
        Long userId = 1L;
        User user = createUser(userId, "사용자");
        Workspace workspace = createWorkspace(workspaceId, "워크스페이스", user);
        MediaFile photo = createMediaFile(user, workspace);
        ReflectionTestUtils.setField(photo, "id", photoId);
        photo.selectAsFinal();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(workspaceRepository.findByIdAndDeletedAtIsNullForUpdate(workspaceId)).willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.findByWorkspaceAndUser(workspace, user))
                .willReturn(Optional.of(WorkspaceMember.builder().build()));
        given(mediaFileRepository.findById(photoId)).willReturn(Optional.of(photo));

        // Act
        workspacePhotoService.deselectFinalPhoto(workspaceId, photoId, userId);

        // Assert
        assertThat(photo.getIsFinal()).isFalse();
        verify(mediaFileRepository).save(photo);
    }

    @Test
    @DisplayName("최종 사진 선택 실패: 사진이 대상 워크스페이스에 속하지 않으면 예외가 발생한다")
    void selectFinalPhoto_Fail_PhotoNotInWorkspace() {
        // Arrange
        Long workspaceId = 1L;
        Long otherWorkspaceId = 2L;
        Long photoId = 10L;
        Long userId = 1L;
        User user = createUser(userId, "사용자");
        Workspace workspace = createWorkspace(workspaceId, "워크스페이스", user);
        Workspace otherWorkspace = createWorkspace(otherWorkspaceId, "다른 워크스페이스", user);
        MediaFile photo = createMediaFile(user, otherWorkspace);
        ReflectionTestUtils.setField(photo, "id", photoId);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(workspaceRepository.findByIdAndDeletedAtIsNullForUpdate(workspaceId)).willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.findByWorkspaceAndUser(workspace, user)).willReturn(Optional.of(WorkspaceMember.builder().build()));
        given(mediaFileRepository.findById(photoId)).willReturn(Optional.of(photo));

        // Act & Assert
        assertThatThrownBy(() -> workspacePhotoService.selectFinalPhoto(workspaceId, photoId, userId))
                .isInstanceOf(WorkspaceException.class)
                .hasFieldOrPropertyWithValue("errorCode", WorkspaceErrorCode.PHOTO_NOT_FOUND);
        verify(mediaFileRepository, never()).clearFinalPhotosExcept(any(), any(), any());
    }

    @Test
    @DisplayName("최종 사진 선택 실패: 워크스페이스가 만료되면 예외가 발생한다")
    void selectFinalPhoto_Fail_ExpiredWorkspace() {
        // Arrange
        Long workspaceId = 1L;
        Long photoId = 10L;
        Long userId = 1L;
        User user = createUser(userId, "사용자");
        Workspace workspace = createWorkspace(workspaceId, "워크스페이스", user);
        workspace.setExpiresAt(LocalDateTime.now().minusDays(1));

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(workspaceRepository.findByIdAndDeletedAtIsNullForUpdate(workspaceId)).willReturn(Optional.of(workspace));

        // Act & Assert
        assertThatThrownBy(() -> workspacePhotoService.selectFinalPhoto(workspaceId, photoId, userId))
                .isInstanceOf(WorkspaceException.class)
                .hasFieldOrPropertyWithValue("errorCode", WorkspaceErrorCode.WORKSPACE_EXPIRED);
        verify(mediaFileRepository, never()).clearFinalPhotosExcept(any(), any(), any());
    }

    @Test
    @DisplayName("최종 사진 선택 실패: 워크스페이스 멤버가 아니면 예외가 발생한다")
    void selectFinalPhoto_Fail_NotWorkspaceMember() {
        // Arrange
        Long workspaceId = 1L;
        Long photoId = 10L;
        Long userId = 1L;
        User user = createUser(userId, "사용자");
        Workspace workspace = createWorkspace(workspaceId, "워크스페이스", user);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(workspaceRepository.findByIdAndDeletedAtIsNullForUpdate(workspaceId)).willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.findByWorkspaceAndUser(workspace, user)).willReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> workspacePhotoService.selectFinalPhoto(workspaceId, photoId, userId))
                .isInstanceOf(WorkspaceException.class)
                .hasFieldOrPropertyWithValue("errorCode", WorkspaceErrorCode.NOT_WORKSPACE_MEMBER);
        verify(mediaFileRepository, never()).clearFinalPhotosExcept(any(), any(), any());
    }

    @Test
    @DisplayName("최종 사진 선택 실패: 사진 타입이 아니면 예외가 발생한다")
    void selectFinalPhoto_Fail_NotPhotoType() {
        // Arrange
        Long workspaceId = 1L;
        Long photoId = 10L;
        Long userId = 1L;
        User user = createUser(userId, "사용자");
        Workspace workspace = createWorkspace(workspaceId, "워크스페이스", user);
        MediaFile mediaFile = createMediaFile(user, workspace);
        ReflectionTestUtils.setField(mediaFile, "id", photoId);
        ReflectionTestUtils.setField(mediaFile, "mediaType", MediaType.VIDEO);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(workspaceRepository.findByIdAndDeletedAtIsNullForUpdate(workspaceId)).willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.findByWorkspaceAndUser(workspace, user)).willReturn(Optional.of(WorkspaceMember.builder().build()));
        given(mediaFileRepository.findById(photoId)).willReturn(Optional.of(mediaFile));

        // Act & Assert
        assertThatThrownBy(() -> workspacePhotoService.selectFinalPhoto(workspaceId, photoId, userId))
                .isInstanceOf(WorkspaceException.class)
                .hasFieldOrPropertyWithValue("errorCode", WorkspaceErrorCode.PHOTO_NOT_FOUND);
        verify(mediaFileRepository, never()).clearFinalPhotosExcept(any(), any(), any());
    }

    @Test
    @DisplayName("댓글 등록 성공")
    void createComment_Success() {
        // Arrange
        Long workspaceId = 1L;
        Long photoId = 10L;
        Long userId = 1L;
        User user = createUser(userId, "유저");
        Workspace workspace = createWorkspace(workspaceId, "워크스페이스", user);
        MediaFile photo = createMediaFile(user, workspace);
        ReflectionTestUtils.setField(photo, "id", photoId);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)).willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.findByWorkspaceAndUser(workspace, user)).willReturn(Optional.of(WorkspaceMember.builder().build()));
        given(mediaFileRepository.findById(photoId)).willReturn(Optional.of(photo));

        WorkspacePhotoCommentRequestDto requestDto = new WorkspacePhotoCommentRequestDto("댓글 내용");

        // Act
        workspacePhotoService.createComment(workspaceId, photoId, requestDto, userId);

        // Assert
        verify(mediaCommentRepository, times(1)).save(any(MediaComment.class));
    }

    @Test
    @DisplayName("댓글 등록 성공: 사진 소유자에게 알림을 발송한다")
    void createComment_NotifiesPhotoOwner() {
        Long workspaceId = 1L;
        Long photoId = 10L;
        Long commenterId = 2L;
        User owner = createUser(1L, "소유자");
        User commenter = createUser(commenterId, "댓글작성자");
        Workspace workspace = createWorkspace(workspaceId, "워크스페이스", owner);
        MediaFile photo = createMediaFile(owner, workspace);
        ReflectionTestUtils.setField(photo, "id", photoId);

        given(userRepository.findById(commenterId)).willReturn(Optional.of(commenter));
        given(workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)).willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.findByWorkspaceAndUser(workspace, commenter))
                .willReturn(Optional.of(WorkspaceMember.builder().workspace(workspace).user(commenter).build()));
        given(mediaFileRepository.findById(photoId)).willReturn(Optional.of(photo));
        given(mediaCommentRepository.save(any(MediaComment.class))).willAnswer(invocation -> {
            MediaComment saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            return saved;
        });

        workspacePhotoService.createComment(
                workspaceId,
                photoId,
                new WorkspacePhotoCommentRequestDto("댓글 내용"),
                commenterId
        );

        verify(notificationService)
                .sendMediaCommentNotification(owner, commenter, workspaceId, photoId, 100L);
    }

    @Test
    @DisplayName("댓글 등록 성공: 자신의 사진에 댓글을 달면 알림을 발송하지 않는다")
    void createComment_DoesNotNotifySelf() {
        Long workspaceId = 1L;
        Long photoId = 10L;
        Long userId = 1L;
        User user = createUser(userId, "소유자");
        Workspace workspace = createWorkspace(workspaceId, "워크스페이스", user);
        MediaFile photo = createMediaFile(user, workspace);
        ReflectionTestUtils.setField(photo, "id", photoId);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)).willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.findByWorkspaceAndUser(workspace, user))
                .willReturn(Optional.of(WorkspaceMember.builder().workspace(workspace).user(user).build()));
        given(mediaFileRepository.findById(photoId)).willReturn(Optional.of(photo));

        workspacePhotoService.createComment(
                workspaceId,
                photoId,
                new WorkspacePhotoCommentRequestDto("댓글 내용"),
                userId
        );

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("댓글 목록 조회 성공")
    void getComments_Success() {
        // Arrange
        Long workspaceId = 1L;
        Long photoId = 10L;
        Long userId = 1L;
        User user = createUser(userId, "유저");
        ReflectionTestUtils.setField(user, "profileImageUrl", "/images/profile/user.png");
        Workspace workspace = createWorkspace(workspaceId, "워크스페이스", user);
        MediaFile photo = createMediaFile(user, workspace);
        ReflectionTestUtils.setField(photo, "id", photoId);

        MediaComment comment = MediaComment.builder()
                .mediaFile(photo)
                .user(user)
                .content("댓글 내용")
                .build();
        ReflectionTestUtils.setField(comment, "id", 100L);
        ReflectionTestUtils.setField(comment, "createdAt", LocalDateTime.now());

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)).willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.findByWorkspaceAndUser(workspace, user)).willReturn(Optional.of(WorkspaceMember.builder().build()));
        given(mediaFileRepository.findById(photoId)).willReturn(Optional.of(photo));
        given(mediaCommentRepository.findAllByMediaFileIdOrderByCreatedAtDesc(photoId)).willReturn(List.of(comment));
        given(profileImageUrlService.toResponseUrl("/images/profile/user.png"))
                .willReturn("http://localhost:8080/images/profile/user.png");

        // Act
        List<com.my4cut.domain.workspace.dto.WorkspacePhotoCommentResponseDto> result = workspacePhotoService.getComments(workspaceId, photoId, userId);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).profileImageUrl()).isEqualTo("http://localhost:8080/images/profile/user.png");
        assertThat(result.get(0).content()).isEqualTo("댓글 내용");
    }

    private User createUser(Long id, String nickname) {
        User user = User.builder().nickname(nickname).build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Workspace createWorkspace(Long id, String name, User owner) {
        Workspace workspace = Workspace.builder().name(name).owner(owner).build();
        ReflectionTestUtils.setField(workspace, "id", id);
        return workspace;
    }

    private MediaFile createMediaFile(User uploader, Workspace workspace) {
        MediaObject mediaObject = MediaObject.builder()
                .owner(uploader)
                .fileKey("url")
                .status(MediaObjectStatus.ACTIVE)
                .build();

        return MediaFile.builder()
                .uploader(uploader)
                .workspace(workspace)
                .mediaObject(mediaObject)
                .fileUrl("url")
                .mediaType(MediaType.PHOTO)
                .isFinal(false)
                .build();
    }
}
