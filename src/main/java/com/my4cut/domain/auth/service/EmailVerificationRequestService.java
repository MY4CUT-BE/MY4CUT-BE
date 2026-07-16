package com.my4cut.domain.auth.service;

import com.my4cut.domain.auth.enums.EmailVerificationPurpose;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.enums.LoginType;
import com.my4cut.domain.user.enums.UserStatus;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.global.exception.BusinessException;
import com.my4cut.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 이메일 인증 요청의 목적에 따라 계정 상태를 검사하고 인증 서비스 호출을 조정한다.
 * 사용자 조회가 끝난 뒤 메일 발송을 호출하여 외부 API 호출 중 DB 트랜잭션이 유지되지 않게 한다.
 */
@Service
@RequiredArgsConstructor
public class EmailVerificationRequestService {

    private final UserRepository userRepository;
    private final EmailVerificationService emailVerificationService;
    private final EmailVerificationRateLimitService rateLimitService;

    /**
     * 회원가입 가능한 이메일에만 인증코드를 발송한다.
     * 탈퇴 계정은 서비스 정책에 따라 재가입할 수 있으므로 발송을 허용한다.
     */
    public void sendSignupCode(String email, String clientAddress) {
        rateLimitService.checkSendAllowed(email, clientAddress);
        boolean duplicated = userRepository.existsByEmailAndStatusNot(email, UserStatus.DELETED);

        if (duplicated) {
            throw new BusinessException(ErrorCode.AUTH_DUPLICATE_EMAIL);
        }

        emailVerificationService.sendCode(email, EmailVerificationPurpose.SIGNUP);
    }

    /**
     * 비밀번호 재설정이 가능한 이메일 로그인 계정에만 인증코드를 발송한다.
     */
    public void sendPasswordResetCode(String email, String clientAddress) {
        rateLimitService.checkSendAllowed(email, clientAddress);
        User user = userRepository.findByEmail(email).orElse(null);

        // 계정 존재 여부와 로그인 방식을 외부 응답으로 구분할 수 없도록 동일하게 성공 처리한다.
        if (user == null || user.isDeleted() || user.getLoginType() != LoginType.EMAIL) {
            return;
        }

        // 입력값이 아니라 계정에 저장된 주소로 발송해 등록 주소 외 수신 가능성을 차단한다.
        emailVerificationService.sendCode(user.getEmail(), EmailVerificationPurpose.PASSWORD_RESET);
    }

    public String verifySignupCode(String email, String code, String clientAddress) {
        rateLimitService.checkVerifyAllowed(clientAddress);
        return emailVerificationService.verifyCode(email, code, EmailVerificationPurpose.SIGNUP);
    }

    /**
     * 비밀번호 재설정 경로에서 발급한 인증코드만 검증한다.
     */
    public String verifyPasswordResetCode(String email, String code, String clientAddress) {
        rateLimitService.checkVerifyAllowed(clientAddress);
        return emailVerificationService.verifyCode(email, code, EmailVerificationPurpose.PASSWORD_RESET);
    }
}
