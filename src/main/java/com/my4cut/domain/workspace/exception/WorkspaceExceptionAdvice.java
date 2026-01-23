package com.my4cut.domain.workspace.exception;

import com.my4cut.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class WorkspaceExceptionAdvice {

    @ExceptionHandler(WorkspaceException.class)
    protected ResponseEntity<ApiResponse<Void>> handleWorkspaceException(WorkspaceException e) {
        log.error("WorkspaceException: {}", e.getErrorCode().getMessage());
        WorkspaceErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.onFailure(errorCode));
    }
}
