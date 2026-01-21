package com.my4cut.domain.workspace.exception;

import com.my4cut.global.common.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum WorkspaceErrorCode implements BaseCode {

    WORKSPACE_NOT_FOUND(HttpStatus.NOT_FOUND, "W4041", "존재하지 않는 워크스페이스입니다."),
    NOT_WORKSPACE_OWNER(HttpStatus.FORBIDDEN, "W4031", "워크스페이스에 대한 권한이 없습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "W4042", "해당 워크스페이스 멤버를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
