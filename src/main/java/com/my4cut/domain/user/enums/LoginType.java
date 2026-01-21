package com.my4cut.domain.user.enums;

/**
 * 사용자의 로그인 방식을 정의하는 열거형이다.
 * 카카오, 네이버, 애플 소셜 로그인과 이메일 로그인을 지원한다.
 */
public enum LoginType {
    KAKAO,
    NAVER,
    APPLE,
    EMAIL
}
