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

    /**
     * Initialize the response using values from the given BaseCode and an optional payload.
     *
     * @param baseCode the BaseCode supplying the response `code` and `message`
     * @param data     the response payload, or `null` if there is no data
     */
    private ApiResponse(BaseCode baseCode, T data) {
        this.code = baseCode.getCode();
        this.message = baseCode.getMessage();
        this.data = data;
    }

    /**
     * Create a success ApiResponse without a payload.
     *
     * @param successCode the BaseCode providing the response `code` and `message`
     * @param <T> the response data type
     * @return an ApiResponse populated with `code` and `message` from successCode and `data` set to null
     */
    public static <T> ApiResponse<T> onSuccess(BaseCode successCode) {
        return new ApiResponse<>(successCode, null);
    }

    /**
     * Create a successful ApiResponse containing the given BaseCode and data.
     *
     * @param successCode the BaseCode whose code and message populate the response
     * @param data the response payload
     * @param <T> the type of the response payload
     * @return an ApiResponse populated with the provided code, message, and data
     */
    public static <T> ApiResponse<T> onSuccess(BaseCode successCode, T data) {
        return new ApiResponse<>(successCode, data);
    }

    /**
     * Create an error ApiResponse without data.
     *
     * @param errorCode the BaseCode representing the error code and message
     * @param <T> the response data type
     * @return an ApiResponse populated with the given error code and no data
     */
    public static <T> ApiResponse<T> onFailure(BaseCode errorCode) {
        return new ApiResponse<>(errorCode, null);
    }

    /**
     * Create an error ApiResponse populated with the given BaseCode and data.
     *
     * @param errorCode the BaseCode that provides the error code and message
     * @param data the response payload to include
     * @param <T> the type of the response payload
     * @return an ApiResponse containing the error code, message, and provided data
     */
    public static <T> ApiResponse<T> onFailure(BaseCode errorCode, T data) {
        return new ApiResponse<>(errorCode, data);
    }
}