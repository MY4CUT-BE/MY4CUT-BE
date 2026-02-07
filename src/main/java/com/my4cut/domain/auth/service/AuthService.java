package com.my4cut.domain.auth.service;

import com.my4cut.domain.auth.dto.AuthResDTO;
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
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

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
            throw new BusinessException(ErrorCode.AUTH_DUPLICATE_EMAIL);
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
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
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

        // DB에 저장된 refreshToken 조회
        RefreshToken savedToken = refreshTokenRepository
                .findByToken(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_REFRESH_TOKEN));

        User user = savedToken.getUser();

        // 탈퇴 유저 방어
        if (user.getStatus() == UserStatus.DELETED) {
            throw new BusinessException(ErrorCode.USER_DELETED);
        }

        // 새 토큰 발급
        String newAccessToken = jwtProvider.createAccessToken(user);
        String newRefreshToken = jwtProvider.createRefreshToken(user);

        // refreshToken 갱신 (rotation)
        savedToken.updateToken(newRefreshToken);

        // 응답
        return UserResDTO.LoginDTO.builder()
                .userId(user.getId())
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    @Transactional
    public void withdraw(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.DELETED) {
            throw new BusinessException(ErrorCode.USER_DELETED);
        }

        // RefreshToken 삭제
        refreshTokenRepository.deleteByUser(user);

        // Soft delete
        user.withdraw();
    }

    // 카카오 로그인
    @Transactional
    public UserResDTO.LoginDTO kakaoLogin(String accessToken) {

        // 카카오 토큰 검증 + 사용자 정보 조회
        AuthResDTO.KakaoUserResDto kakaoUser = getKakaoUser(accessToken);
        String oauthId = kakaoUser.id().toString();

        // 유저 조회 or 생성
        User user = userRepository
                .findByLoginTypeAndOauthId(LoginType.KAKAO, oauthId)
                .orElseGet(() -> createKakaoUser(oauthId));

        // JWT 발급 (EMAIL 로그인과 동일)
        String accessTokenJwt = jwtProvider.createAccessToken(user);
        String refreshToken = jwtProvider.createRefreshToken(user);

        refreshTokenRepository.findByUser(user)
                .ifPresentOrElse(
                        rt -> rt.updateToken(refreshToken),
                        () -> refreshTokenRepository.save(new RefreshToken(user, refreshToken))
                );

        return UserResDTO.LoginDTO.builder()
                .userId(user.getId())
                .accessToken(accessTokenJwt)
                .refreshToken(refreshToken)
                .build();
    }

    //카카오 API 호출 (분리)
    private AuthResDTO.KakaoUserResDto getKakaoUser(String accessToken) {

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<AuthResDTO.KakaoUserResDto> response =
                restTemplate.exchange(
                        "https://kapi.kakao.com/v2/user/me",
                        HttpMethod.GET,
                        entity,
                        AuthResDTO.KakaoUserResDto.class
                );

        AuthResDTO.KakaoUserResDto body = response.getBody();

        if (body == null || body.id() == null) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_KAKAO_RESPONSE);
        }

        return body;
    }

    private User createKakaoUser(String oauthId) {

        String friendCode = UUID.randomUUID()
                .toString()
                .substring(0, 6)
                .toUpperCase();

        User user = User.builder()
                .loginType(LoginType.KAKAO)
                .oauthId(oauthId)
                .email(null)
                .password(null)
                .nickname("kakao_user")
                .friendCode(friendCode)
                .status(UserStatus.ACTIVE)
                .build();

        return userRepository.save(user);
    }


}
