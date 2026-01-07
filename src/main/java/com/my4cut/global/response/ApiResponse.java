package com.my4cut.global.response;

import com.my4cut.global.common.BaseCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공통 응답 형식을 정의하는 클래스.
 *
 * @param <T> 응답 데이터의 타입
 * @author koohyunmo
 * @since 2026-01-07
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {

    private String code;
    private String message;
    private T data;

    // BaseCode 인터페이스를 사용하는 공통 생성자
    private ApiResponse(BaseCode baseCode, T data) {
        this.code = baseCode.getCode();
        this.message = baseCode.getMessage();
        this.data = data;
    }

    /**
     * 성공 응답을 생성합니다 (데이터 없음).
     *
     * @param successCode 성공 코드
     * @param <T> 데이터 타입
     * @return ApiResponse 객체
     */
    public static <T> ApiResponse<T> onSuccess(SuccessCode successCode) {
        return new ApiResponse<>(successCode, null);
    }

    /**
     * 성공 응답을 생성합니다 (데이터 포함).
     *
     * @param successCode 성공 코드
     * @param data 응답 데이터
     * @param <T> 데이터 타입
     * @return ApiResponse 객체
     */
    public static <T> ApiResponse<T> onSuccess(SuccessCode successCode, T data) {
        return new ApiResponse<>(successCode, data);
    }

    /**
     * 에러 응답을 생성합니다 (데이터 없음).
     *
     * @param errorCode 에러 코드
     * @param <T> 데이터 타입
     * @return ApiResponse 객체
     */
    public static <T> ApiResponse<T> onFailure(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode, null);
    }

    /**
     * 에러 응답을 생성합니다 (데이터 포함).
     *
     * @param errorCode 에러 코드
     * @param data 응답 데이터
     * @param <T> 데이터 타입
     * @return ApiResponse 객체
     */
    public static <T> ApiResponse<T> onFailure(ErrorCode errorCode, T data) {
        return new ApiResponse<>(errorCode, data);
    }
}