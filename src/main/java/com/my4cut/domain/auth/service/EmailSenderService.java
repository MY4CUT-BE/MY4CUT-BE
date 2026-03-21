package com.my4cut.domain.auth.service;

/*
 * 이메일 발송 추상화 인터페이스
 */
public interface EmailSenderService {

    void sendVerificationCode(String toEmail, String code);
}