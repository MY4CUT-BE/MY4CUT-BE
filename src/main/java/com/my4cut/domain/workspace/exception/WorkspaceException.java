package com.my4cut.domain.workspace.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class WorkspaceException extends RuntimeException {
    private final WorkspaceErrorCode errorCode;
}
