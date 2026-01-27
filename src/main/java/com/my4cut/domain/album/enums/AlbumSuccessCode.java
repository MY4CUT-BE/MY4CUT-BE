package com.my4cut.domain.album.enums;

import com.my4cut.global.common.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AlbumSuccessCode implements BaseCode {

    ALBUM_CREATE_SUCCESS(HttpStatus.CREATED, "A2001", "앨범이 생성되었습니다."),
    ALBUM_LIST_GET_SUCCESS(HttpStatus.OK, "A2002", "앨범 목록을 조회했습니다."),
    ALBUM_DETAIL_GET_SUCCESS(HttpStatus.OK, "A2003", "앨범 상세 정보를 조회했습니다."),
    ALBUM_UPDATE_SUCCESS(HttpStatus.OK, "A2004", "앨범 이름을 수정했습니다."),
    ALBUM_DELETE_SUCCESS(HttpStatus.OK, "A2005", "앨범을 삭제했습니다."),
    ALBUM_PHOTO_UPDATE_SUCCESS(HttpStatus.OK, "A2006", "앨범 사진 구성을 수정했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
