package com.my4cut.domain.pose.dto.req;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PoseCreateRequest(
        @NotBlank
        @Size(max = 255)
        String title,

        @NotNull
        @Min(1)
        @Max(10)
        Integer peopleCount
) {
}
