package com.my4cut.domain.album.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AlbumException extends RuntimeException {
    private final AlbumErrorCode errorCode;
}
