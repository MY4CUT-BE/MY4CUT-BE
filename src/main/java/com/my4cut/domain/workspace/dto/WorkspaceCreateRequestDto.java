package com.my4cut.domain.workspace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Schema(description = "워크스페이스 생성 요청 DTO")
public record WorkspaceCreateRequestDto(
        @Schema(description = "워크스페이스 이름", example = "우리 가족 앨범")
        @NotBlank(message = "워크스페이스 이름은 비어 있을 수 없습니다.")
        @Size(max = 15, message = "워크스페이스 이름은 최대 15자입니다.")
        @Pattern(
                regexp = "^[가-힣ㄱ-ㅎa-zA-Z0-9]+$",
                message = "워크스페이스 이름은 한글, 영문, 숫자만 사용할 수 있습니다."
        )
        String name
) {}
