package com.warrantypro.auth.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SliderCaptchaVerifyRequest(
        @NotBlank String challengeId,
        @NotNull @Min(0) @Max(320) Integer sliderOffset,
        /** 可选：前端采集的拖动轨迹 JSON，例如 {"durationMs":412,"points":[[0,0],[0.03,12],...]}。 */
        String trajectory
) {
}
