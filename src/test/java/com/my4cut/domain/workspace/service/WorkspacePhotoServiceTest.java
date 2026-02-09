package com.my4cut.domain.workspace.service;

import com.my4cut.domain.image.service.ImageStorageService;
import com.my4cut.domain.media.entity.MediaComment;
import com.my4cut.domain.media.entity.MediaFile;
import com.my4cut.domain.media.enums.MediaType;
import com.my4cut.domain.media.repository.MediaCommentRepository;
import com.my4cut.domain.media.repository.MediaFileRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

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
        MediaFile mediaFile = MediaFile.builder().uploader(user).fileUrl("url").mediaType(MediaType.PHOTO).build();
        ReflectionTestUtils.setField(mediaFile, "id", mediaId);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(workspaceRepository.findById(workspaceId)).willReturn(Optional.of(workspace));
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
    @DisplayName("사진 삭제 성공: 본인이 올린 사진을 삭제한다")
    void deletePhoto_Success() {
        // Arrange
        Long workspaceId = 1L;
        Long photoId = 10L;
        Long userId = 1L;
        User user = createUser(userId, "유저");
        Workspace workspace = createWorkspace(workspaceId, "워크스페이스", user);
        MediaFile photo = MediaFile.builder().uploader(user).workspace(workspace).fileUrl("url").build();

        given(workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)).willReturn(Optional.of(workspace));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(workspaceRepository.findById(workspaceId)).willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.findByWorkspaceAndUser(workspace, user)).willReturn(Optional.of(WorkspaceMember.builder().build()));
        given(mediaFileRepository.findById(photoId)).willReturn(Optional.of(photo));

        // Act
        workspacePhotoService.deletePhoto(workspaceId, photoId, userId);

        // Assert
        verify(imageStorageService).deleteIfExists("url");
        verify(mediaFileRepository).delete(photo);
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
        MediaFile photo = MediaFile.builder().uploader(user).workspace(workspace).build();
        ReflectionTestUtils.setField(photo, "id", photoId);

        given(workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)).willReturn(Optional.of(workspace));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(workspaceRepository.findById(workspaceId)).willReturn(Optional.of(workspace));
        given(workspaceMemberRepository.findByWorkspaceAndUser(workspace, user)).willReturn(Optional.of(WorkspaceMember.builder().build()));
        given(mediaFileRepository.findById(photoId)).willReturn(Optional.of(photo));

        WorkspacePhotoCommentRequestDto requestDto = new WorkspacePhotoCommentRequestDto("댓글 내용");

        // Act
        workspacePhotoService.createComment(workspaceId, photoId, requestDto, userId);

        // Assert
        verify(mediaCommentRepository, times(1)).save(any(MediaComment.class));
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
}
