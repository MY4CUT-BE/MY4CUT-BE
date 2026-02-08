package com.my4cut.domain.album.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class AlbumRequestDto {

    @Schema(description = "앨범 생성/수정 요청 DTO")
    public record CreateOrUpdate(
            @NotBlank(message = "앨범 이름은 필수입니다.")
            @Schema(description = "앨범 이름", example = "제주도 여행 앨범")
            String name
    ) {}

        @Schema(description = "앨범 미디어 추가/제외 요청 DTO")

        public record UpdateMedia(

                @NotEmpty(message = "미디어 ID 리스트는 비어있을 수 없습니다.")

                @Schema(description = "미디어 ID 리스트", example = "[1, 2, 3]")

                List<Long> mediaIds

        ) {}

    
}
