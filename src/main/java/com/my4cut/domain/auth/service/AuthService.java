package com.my4cut.domain.auth.service;

import com.my4cut.domain.auth.entity.RefreshToken;
import com.my4cut.domain.auth.jwt.JwtProvider;
import com.my4cut.domain.auth.repository.RefreshTokenRepository;
import com.my4cut.domain.user.dto.UserReqDTO;
import com.my4cut.domain.user.dto.UserResDTO;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.enums.LoginType;
import com.my4cut.domain.user.enums.UserStatus;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.global.exception.BusinessException;
import com.my4cut.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;
    private final BCryptPasswordEncoder passwordEncoder;

    // 회원가입
    @Transactional
    public void signup(UserReqDTO.SignUpDTO request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        String friendCode = UUID.randomUUID()
                .toString()
                .substring(0, 6)
                .toUpperCase();

        User user = User.builder()
                .email(request.email())
                .password(encodedPassword)
                .nickname(request.nickname())
                .profileImageUrl(null)
                .loginType(LoginType.EMAIL)
                .friendCode(friendCode)
                .status(UserStatus.ACTIVE)
                .build();

        userRepository.save(user);
    }

    // 로그인
    @Transactional
    public UserResDTO.LoginDTO login(UserReqDTO.LoginDTO request) {

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        String accessToken = jwtProvider.createAccessToken(user);
        String refreshToken = jwtProvider.createRefreshToken(user);

        refreshTokenRepository.findByUser(user)
                .ifPresentOrElse(
                        rt -> rt.updateToken(refreshToken),
                        () -> refreshTokenRepository.save(new RefreshToken(user, refreshToken))
                );

        return UserResDTO.LoginDTO.builder()
                .userId(user.getId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    // 토큰 재발급
    @Transactional
    public UserResDTO.LoginDTO refresh(String refreshToken) {

        // 1️⃣ DB에 저장된 refreshToken 조회
        RefreshToken savedToken = refreshTokenRepository
                .findByToken(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        User user = savedToken.getUser();

        // 2️⃣ 탈퇴 유저 방어
        if (user.getStatus() == UserStatus.DELETED) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 3️⃣ 새 토큰 발급
        String newAccessToken = jwtProvider.createAccessToken(user);
        String newRefreshToken = jwtProvider.createRefreshToken(user);

        // 4️⃣ refreshToken 갱신 (rotation)
        savedToken.updateToken(newRefreshToken);

        // 5️⃣ 응답
        return UserResDTO.LoginDTO.builder()
                .userId(user.getId())
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    @Transactional
    public void withdraw(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        if (user.getStatus() == UserStatus.DELETED) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // RefreshToken 삭제
        refreshTokenRepository.deleteByUser(user);

        // Soft delete
        user.withdraw();
    }


}
