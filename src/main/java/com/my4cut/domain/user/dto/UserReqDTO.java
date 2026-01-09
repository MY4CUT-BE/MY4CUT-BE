package com.my4cut.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserReqDTO {

    // 로그인
    public record LoginDTO(
            @NotBlank
            String email,
            @NotBlank
            String password
    ){}
}
