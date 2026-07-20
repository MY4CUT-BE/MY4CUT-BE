package com.my4cut.domain.pose.dto.req;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record PoseUpdateRequest(
        @Size(max = 255)
        String title,

        @Min(1)
        @Max(10)
        Integer peopleCount
) {
}
