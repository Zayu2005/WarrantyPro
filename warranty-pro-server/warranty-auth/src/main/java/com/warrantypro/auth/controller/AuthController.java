package com.warrantypro.auth.controller;

import com.warrantypro.auth.dto.LoginRequest;
import com.warrantypro.auth.dto.LoginResponse;
import com.warrantypro.auth.dto.RefreshBody;
import com.warrantypro.auth.dto.SliderCaptchaVO;
import com.warrantypro.auth.dto.SliderCaptchaVerifyRequest;
import com.warrantypro.auth.dto.UserInfoVO;
import com.warrantypro.auth.service.AuthService;
import com.warrantypro.auth.service.SliderCaptchaService;
import com.warrantypro.common.result.Result;
import com.warrantypro.common.security.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 认证接口（docs/07 §2.1）。 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SliderCaptchaService sliderCaptchaService;

    @GetMapping("/slider-captcha")
    public Result<SliderCaptchaVO> sliderCaptcha() {
        return Result.ok(sliderCaptchaService.issue());
    }

    @PostMapping("/slider-captcha/verify")
    public Result<Void> verifySliderCaptcha(@Valid @RequestBody SliderCaptchaVerifyRequest request) {
        sliderCaptchaService.verify(request.challengeId(), request.sliderOffset(), request.trajectory());
        return Result.ok();
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@Valid @RequestBody RefreshBody body) {
        return Result.ok(authService.refresh(body.refreshToken()));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authorization) {
        String token = authorization.startsWith("Bearer ") ? authorization.substring(7) : authorization;
        authService.logout(token);
        return Result.ok();
    }

    @GetMapping("/me")
    public Result<UserInfoVO> me(@AuthenticationPrincipal LoginUser user) {
        return Result.ok(authService.me(user));
    }
}
