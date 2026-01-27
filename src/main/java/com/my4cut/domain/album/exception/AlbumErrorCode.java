package com.my4cut.domain.album.exception;

import com.my4cut.global.common.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AlbumErrorCode implements BaseCode {

    ALBUM_NOT_FOUND(HttpStatus.NOT_FOUND, "A4041", "존재하지 않는 앨범입니다."),
    NOT_ALBUM_OWNER(HttpStatus.FORBIDDEN, "A4031", "해당 앨범에 대한 권한이 없습니다."),
    MEDIA_ALREADY_IN_ALBUM(HttpStatus.BAD_REQUEST, "A4001", "이미 해당 앨범에 속해 있는 미디어입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
