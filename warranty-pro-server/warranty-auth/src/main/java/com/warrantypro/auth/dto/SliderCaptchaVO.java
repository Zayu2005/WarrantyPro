package com.warrantypro.auth.dto;

public record SliderCaptchaVO(
        String challengeId,
        String backgroundImage,
        String pieceImage,
        int imageWidth,
        int imageHeight,
        int pieceY,
        int pieceSize,
        int trackWidth
) {
}
