package com.my4cut.domain.workspace.enums;

import com.my4cut.global.common.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum WorkspaceSuccessCode implements BaseCode {

    WORKSPACE_GET_SUCCESS(HttpStatus.OK, "W2000", "워크스페이스 정보가 성공적으로 조회되었습니다."),
    WORKSPACE_CREATED(HttpStatus.CREATED, "W2011", "워크스페이스가 성공적으로 생성되었습니다."),
    WORKSPACE_MODIFIED(HttpStatus.OK, "W2001", "워크스페이스 정보가 수정되었습니다."),
    WORKSPACE_DELETED(HttpStatus.OK, "W2002", "워크스페이스가 삭제되었습니다."),
    MEMBER_DELETED(HttpStatus.OK, "W2003", "멤버가 성공적으로 삭제되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
