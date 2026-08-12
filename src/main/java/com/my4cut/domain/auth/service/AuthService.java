package com.my4cut.domain.auth.service;

import com.my4cut.domain.auth.dto.req.AuthReqDTO;
import com.my4cut.domain.auth.dto.res.AuthResDTO;
import com.my4cut.domain.auth.entity.RefreshToken;
import com.my4cut.domain.auth.enums.EmailVerificationPurpose;
import com.my4cut.domain.auth.jwt.JwtProvider;
import com.my4cut.domain.auth.repository.RefreshTokenRepository;
import com.my4cut.domain.image.ImageConstants;
import com.my4cut.domain.tutorial.service.TutorialService;
import com.my4cut.domain.user.dto.UserReqDTO;
import com.my4cut.domain.user.dto.UserResDTO;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.enums.LoginType;
import com.my4cut.domain.user.enums.UserStatus;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.domain.workspace.service.WorkspaceService;
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

import java.util.regex.Pattern;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final Pattern PASSWORD_POLICY_PATTERN =
            Pattern.compile("^(?=\\S{8,64}$)(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d\\s]).*$");

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final WorkspaceService workspaceService;
    private final TutorialService tutorialService;

    @Transactional(readOnly = true)
    public AuthResDTO.CheckEmailResDto checkEmailDuplicate(String email) {
        boolean duplicated =
                userRepository.existsByEmailAndStatusNot(email, UserStatus.DELETED);
        return new AuthResDTO.CheckEmailResDto(email, duplicated);
    }

    // 회원가입
    @Transactional
    public void signup(UserReqDTO.SignUpDTO request) {
        if (!emailVerificationService.claimVerifiedForTransaction(
                request.email(),
                EmailVerificationPurpose.SIGNUP,
                request.verificationToken()
        )) {
            throw new BusinessException(ErrorCode.AUTH_EMAIL_NOT_VERIFIED);
        }

        User existingUser = userRepository.findByEmail(request.email()).orElse(null);

        //현재 있는 유저인지 조회
        if (existingUser != null && !existingUser.isDeleted()) {
            throw new BusinessException(ErrorCode.AUTH_DUPLICATE_EMAIL);
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        //재가입시
        if (existingUser != null) {
            existingUser.activateEmailLogin(request.email());
            existingUser.updatePassword(encodedPassword);
            existingUser.updateNickname(request.nickname());
            existingUser.reactivate();
            refreshTokenRepository.deleteByUser(existingUser);
            return;
        }

        String friendCode = UUID.randomUUID()
                .toString()
                .substring(0, 6)
                .toUpperCase();

        User user = User.builder()
                .email(request.email())
                .password(encodedPassword)
                .nickname(request.nickname())
                .profileImageUrl(ImageConstants.DEFAULT_PROFILE_IMAGE_URL)
                .loginType(LoginType.EMAIL)
                .friendCode(friendCode)
                .status(UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);
        workspaceService.createDefaultWorkspace(savedUser);
        tutorialService.initialize(savedUser);
    }

    // 로그인
    @Transactional
    public UserResDTO.LoginDTO login(UserReqDTO.LoginDTO request) {

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));

        if (user.isDeleted()) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

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
        if (user.isDeleted()) {
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

        if (user.isDeleted()) {
            throw new BusinessException(ErrorCode.USER_DELETED);
        }

        // RefreshToken 삭제
        refreshTokenRepository.deleteByUser(user);

        // Soft delete
        user.withdraw();
    }

    //비밀번호 재설정
    @Transactional
    public void resetPassword(AuthReqDTO.ResetPasswordReqDto request) {
        // 비밀번호 재설정 목적으로 완료한 이메일 인증인지 확인한다.
        if (!emailVerificationService.claimVerifiedForTransaction(
                request.email(),
                EmailVerificationPurpose.PASSWORD_RESET,
                request.verificationToken()
        )) {
            throw new BusinessException(ErrorCode.AUTH_EMAIL_NOT_VERIFIED);
        }

        if (!PASSWORD_POLICY_PATTERN.matcher(request.newPassword()).matches()) {    //비밀번호 정책 확인
            throw new BusinessException(ErrorCode.AUTH_PASSWORD_POLICY_VIOLATION);
        }

        User user = userRepository.findByEmail(request.email()) // 이메일로 사용자 검증
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));

        if (user.isDeleted()) { //탈퇴한 유저일 경우
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        if (user.getLoginType() != LoginType.EMAIL) {   //카카오 로그인일 경우 비밀번호 교체 불가
            throw new BusinessException(ErrorCode.AUTH_PASSWORD_RESET_NOT_ALLOWED);
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {   //이전 비밀번호와 동일한지 비교
            throw new BusinessException(ErrorCode.AUTH_PASSWORD_SAME_AS_OLD);
        }

        user.updatePassword(passwordEncoder.encode(request.newPassword())); //비밀번호 갱신
        refreshTokenRepository.deleteByUser(user);  //리프레시 토큰 제거(기존 로그인 상태 무효화)
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
                .map(existingUser -> {
                    if (existingUser.isDeleted()) {
                        existingUser.reactivate();
                        refreshTokenRepository.deleteByUser(existingUser);
                    }
                    return existingUser;
                })
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
    AuthResDTO.KakaoUserResDto getKakaoUser(String accessToken) {

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
                .profileImageUrl(ImageConstants.DEFAULT_PROFILE_IMAGE_URL)
                .friendCode(friendCode)
                .status(UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);
        workspaceService.createDefaultWorkspace(savedUser);
        tutorialService.initialize(savedUser);
        return savedUser;
    }


}
