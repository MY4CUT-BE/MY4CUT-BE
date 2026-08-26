package com.my4cut.domain.tutorial.dto;

import com.my4cut.domain.tutorial.enums.TutorialType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Schema(description = "사용자별 튜토리얼 완료 상태")
public record TutorialStatusResponseDto(
        @Schema(description = "전체 튜토리얼 완료 상태")
        List<TutorialStatus> tutorials
) {
    public static TutorialStatusResponseDto from(Set<TutorialType> completedTypes) {
        List<TutorialStatus> statuses = Arrays.stream(TutorialType.values())
                .map(type -> new TutorialStatus(type, completedTypes.contains(type)))
                .toList();
        return new TutorialStatusResponseDto(statuses);
    }

    public record TutorialStatus(
            @Schema(description = "튜토리얼 유형", example = "HOME")
            TutorialType type,
            @Schema(description = "완료 여부", example = "true")
            boolean completed
    ) {
    }
}
