package com.my4cut.domain.user.service;

import com.my4cut.domain.auth.jwt.JwtProvider;
import com.my4cut.domain.user.dto.UserReqDTO;
import com.my4cut.domain.user.dto.UserResDTO;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.global.exception.BusinessException;
import com.my4cut.global.response.ErrorCode;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            JwtProvider jwtProvider,
            BCryptPasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.jwtProvider = jwtProvider;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResDTO.LoginDTO login(UserReqDTO.LoginDTO request) {

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.NOT_FOUND)
                );

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        String token = jwtProvider.createToken(user.getId());
        return new UserResDTO.LoginDTO(user.getId(), token);
    }
}