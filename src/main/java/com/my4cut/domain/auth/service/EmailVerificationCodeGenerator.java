package com.my4cut.domain.auth.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/*
 * 이메일 인증코드 생성기
 *
 * - 100000 ~ 999999 사이의 숫자를 생성하여
 *   항상 6자리 인증코드가 나오도록 한다.
 */
@Component
public class EmailVerificationCodeGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public String generate() {
        return String.valueOf(100000 + SECURE_RANDOM.nextInt(900000));
    }
}
