package com.serdarahmanov.music_app_backend.auth.controller;

import com.serdarahmanov.music_app_backend.auth.forcodex.dto.Oauth2ExchangeRequest;
import com.serdarahmanov.music_app_backend.auth.forcodex.service.Oauth2ExchangeService;
import com.serdarahmanov.music_app_backend.auth.forgotEmail.UpdatePasswordRequest;
import com.serdarahmanov.music_app_backend.auth.service.AuthService;
import com.serdarahmanov.music_app_backend.auth.dto.AuthMeResponse;
import com.serdarahmanov.music_app_backend.auth.dto.AuthTokenResponse;
import com.serdarahmanov.music_app_backend.auth.dto.CreateUserRequest;
import com.serdarahmanov.music_app_backend.auth.dto.RefreshTokenRequest;
import com.serdarahmanov.music_app_backend.auth.dto.ResendVerificationRequest;
import com.serdarahmanov.music_app_backend.auth.dto.SessionInfoResponse;
import com.serdarahmanov.music_app_backend.auth.dto.UserNameAndPassword;
import com.serdarahmanov.music_app_backend.auth.dto.UserResponse;
import com.serdarahmanov.music_app_backend.auth.forgotEmail.ForgotEmailRequest;
import com.serdarahmanov.music_app_backend.auth.forgotEmail.ResetPasswordRequest;
import com.serdarahmanov.music_app_backend.utility.ApplicationProperties;
import com.serdarahmanov.music_app_backend.utility.api.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final ApplicationProperties applicationProperties;
    private final Oauth2ExchangeService exchangeService;
    private final ClientIpResolver clientIpResolver;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody CreateUserRequest request
    ) {
        return ResponseEntity.ok(authService.createUser(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthTokenResponse> login(
            @Valid @RequestBody UserNameAndPassword request,
            HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(
                authService.login(
                        request,
                        httpServletRequest.getHeader("User-Agent"),
                        clientIpResolver.resolve(httpServletRequest)
                )
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<?> logoutAll() {
        authService.logoutAll();
        return ResponseEntity.ok(Map.of("message", "Logged out from all sessions"));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<SessionInfoResponse>> sessions() {
        return ResponseEntity.ok(authService.getMySessions());
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<?> revokeSession(@PathVariable Long sessionId) {
        authService.revokeMySession(sessionId);
        return ResponseEntity.ok(Map.of("message", "Session revoked"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthTokenResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(
                authService.refresh(
                        request,
                        httpServletRequest.getHeader("User-Agent"),
                        clientIpResolver.resolve(httpServletRequest)
                )
        );
    }

    @GetMapping("/me")
    public ResponseEntity<AuthMeResponse> me() {
        return ResponseEntity.ok(authService.getMe());
    }

    @GetMapping("/verify-email")
    public RedirectView verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return new RedirectView(applicationProperties.getLoginPageUrl());
    }

    @PostMapping("/verify-email/resend")
    public ResponseEntity<?> resendVerificationEmail(
            @Valid @RequestBody ResendVerificationRequest request
    ) {
        authService.resendVerificationEmail(request.email());
        return ResponseEntity.ok("If this account exists and is unverified, a verification email has been sent.");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(
            @Valid @RequestBody ForgotEmailRequest request
    ) {
        authService.handleForgotPassword(request.getEmail());
        return ResponseEntity.ok("If this email exists, a reset link has been sent.");
    }

    @GetMapping("/forgot-password")
    public RedirectView validateResetToken(@RequestParam String code) {
        authService.validateResetToken(code);
        return new RedirectView(
                applicationProperties.getFrontendResetPasswordUrl() + "?code=" + code
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        authService.resetPassword(request.getCode(), request.getPassword());
        return ResponseEntity.ok("Password successfully reset");
    }


    @PostMapping("/password/set-request")
    public ResponseEntity<?> requestPasswordSet() {
        authService.requestPasswordSet();

        return ResponseEntity.ok("Password setup email sent");
    }

    @PostMapping("/update-password")
    public ResponseEntity<?> updatePassword(
            @Valid @RequestBody UpdatePasswordRequest request
    ) {
        authService.updatePasswordWithOldPassword(request.getOldPassword(), request.getPassword());
        return ResponseEntity.ok("Password successfully reset");
    }

    @PostMapping("/oauth2/exchange")
    public ResponseEntity<AuthTokenResponse> exchangeOauth2Code(
            @Valid @RequestBody Oauth2ExchangeRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(
                exchangeService.exchangeCodeForToken(
                        request.getCode(),
                        httpServletRequest.getHeader("User-Agent"),
                        clientIpResolver.resolve(httpServletRequest)
                )
        );
    }

}
