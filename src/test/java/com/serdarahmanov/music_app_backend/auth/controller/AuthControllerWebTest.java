package com.serdarahmanov.music_app_backend.auth.controller;

import com.serdarahmanov.music_app_backend.auth.dto.AuthTokenResponse;
import com.serdarahmanov.music_app_backend.auth.dto.RefreshTokenRequest;
import com.serdarahmanov.music_app_backend.auth.dto.SessionInfoResponse;
import com.serdarahmanov.music_app_backend.auth.jwt.JwtFilter;
import com.serdarahmanov.music_app_backend.auth.forcodex.service.Oauth2ExchangeService;
import com.serdarahmanov.music_app_backend.auth.refresh.exceptions.RefreshTokenExpiredException;
import com.serdarahmanov.music_app_backend.auth.refresh.exceptions.RefreshTokenInvalidException;
import com.serdarahmanov.music_app_backend.auth.refresh.exceptions.RefreshTokenOwnershipException;
import com.serdarahmanov.music_app_backend.auth.refresh.exceptions.RefreshTokenReuseDetectedException;
import com.serdarahmanov.music_app_backend.auth.security.LoginRateLimitExceededException;
import com.serdarahmanov.music_app_backend.auth.service.AuthService;
import com.serdarahmanov.music_app_backend.auth.config.Oauth2LoginSuccessHandler;
import com.serdarahmanov.music_app_backend.utility.ApplicationProperties;
import com.serdarahmanov.music_app_backend.utility.api.ClientIpResolver;
import com.serdarahmanov.music_app_backend.utility.config.AppSecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import com.serdarahmanov.music_app_backend.utility.customExceptions.ResetPasswordTokenNotExistException;
import com.serdarahmanov.music_app_backend.utility.customExceptions.VerificationCodeNotFoundException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.stream.Stream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private Oauth2ExchangeService oauth2ExchangeService;

    @MockitoBean
    private ApplicationProperties applicationProperties;

    @MockitoBean
    private JwtFilter jwtFilter;

    @MockitoBean
    private Oauth2LoginSuccessHandler oauth2LoginSuccessHandler;

    @MockitoBean
    private ClientIpResolver clientIpResolver;

    @MockitoBean
    private AppSecurityProperties appSecurityProperties;

    @BeforeEach
    void setUp() {
        when(appSecurityProperties.getAllowedOrigins()).thenReturn(List.of("http://localhost:3000"));
        when(clientIpResolver.resolve(any(HttpServletRequest.class)))
                .thenAnswer(invocation -> {
                    HttpServletRequest request = invocation.getArgument(0);
                    String xForwardedFor = request.getHeader("X-Forwarded-For");
                    if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                        return xForwardedFor.split(",")[0].trim();
                    }
                    return request.getRemoteAddr();
                });
    }

    @Test
    void loginReturnsAccessAndRefreshTokensAsJson() throws Exception {
        when(authService.login(org.mockito.ArgumentMatchers.any(), eq("JUnit"), eq("10.0.0.11")))
                .thenReturn(new AuthTokenResponse(
                        "access-login",
                        "Bearer",
                        3_600_000L,
                        "refresh-login",
                        1_209_600_000L
                ));

        mockMvc.perform(post("/api/auth/login")
                        .header("User-Agent", "JUnit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"secret\"}")
                        .with(request -> {
                            request.setRemoteAddr("10.0.0.11");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-login"))
                .andExpect(jsonPath("$.accessTokenType").value("Bearer"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-login"));

        ArgumentCaptor<com.serdarahmanov.music_app_backend.auth.dto.UserNameAndPassword> captor =
                ArgumentCaptor.forClass(com.serdarahmanov.music_app_backend.auth.dto.UserNameAndPassword.class);
        verify(authService).login(captor.capture(), eq("JUnit"), eq("10.0.0.11"));
        assertEquals("alice", captor.getValue().getUsername());
    }

    @Test
    void loginReturnsUnauthorizedForInvalidCredentials() throws Exception {
        when(authService.login(any(), eq("JUnit"), eq("10.0.0.11")))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .header("User-Agent", "JUnit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"wrong\"}")
                        .with(request -> {
                            request.setRemoteAddr("10.0.0.11");
                            return request;
                        }))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid credentials"))
                .andExpect(jsonPath("$.traceId").isString());
    }

    @Test
    void refreshReturnsRotatedTokenPairAsJson() throws Exception {
        when(authService.refresh(org.mockito.ArgumentMatchers.any(), eq("JUnit"), eq("10.0.0.12")))
                .thenReturn(new AuthTokenResponse(
                        "access-refresh",
                        "Bearer",
                        3_600_000L,
                        "refresh-rotated",
                        1_209_600_000L
                ));

        mockMvc.perform(post("/api/auth/refresh")
                        .header("User-Agent", "JUnit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"old-refresh\"}")
                        .with(request -> {
                            request.setRemoteAddr("10.0.0.12");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-refresh"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-rotated"))
                .andExpect(jsonPath("$.refreshTokenExpiresInMs").value(1_209_600_000L));
    }

    @Test
    void loginReturnsTooManyRequestsWhenRateLimited() throws Exception {
        when(authService.login(any(), eq("JUnit"), eq("10.0.0.11")))
                .thenThrow(new LoginRateLimitExceededException("Too many failed login attempts. Try again later."));

        mockMvc.perform(post("/api/auth/login")
                        .header("User-Agent", "JUnit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"wrong\"}")
                        .with(request -> {
                            request.setRemoteAddr("10.0.0.11");
                            return request;
                        }))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
                .andExpect(jsonPath("$.message").value("Too many failed login attempts. Try again later."))
                .andExpect(jsonPath("$.traceId").isString());
    }

    @Test
    void loginReturnsForbiddenWhenAccountDisabled() throws Exception {
        when(authService.login(any(), eq("JUnit"), eq("10.0.0.11")))
                .thenThrow(new DisabledException("User account is disabled"));

        mockMvc.perform(post("/api/auth/login")
                        .header("User-Agent", "JUnit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"secret\"}")
                        .with(request -> {
                            request.setRemoteAddr("10.0.0.11");
                            return request;
                        }))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"))
                .andExpect(jsonPath("$.message").value("User account is disabled"))
                .andExpect(jsonPath("$.traceId").isString());
    }

    @Test
    void refreshReturnsUnauthorizedForExpiredToken() throws Exception {
        when(authService.refresh(any(), eq("JUnit"), eq("10.0.0.12")))
                .thenThrow(new RefreshTokenExpiredException("Refresh token has expired"));

        mockMvc.perform(post("/api/auth/refresh")
                        .header("User-Agent", "JUnit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"expired-token\"}")
                        .with(request -> {
                            request.setRemoteAddr("10.0.0.12");
                            return request;
                        }))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_EXPIRED"))
                .andExpect(jsonPath("$.message").value("Refresh token has expired"))
                .andExpect(jsonPath("$.traceId").isString());
    }

    @Test
    void refreshReturnsUnauthorizedForReusedToken() throws Exception {
        when(authService.refresh(any(), eq("JUnit"), eq("10.0.0.12")))
                .thenThrow(new RefreshTokenReuseDetectedException("Refresh token reuse detected"));

        mockMvc.perform(post("/api/auth/refresh")
                        .header("User-Agent", "JUnit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"reused-token\"}")
                        .with(request -> {
                            request.setRemoteAddr("10.0.0.12");
                            return request;
                        }))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_REUSED"))
                .andExpect(jsonPath("$.message").value("Refresh token reuse detected"))
                .andExpect(jsonPath("$.traceId").isString());
    }

    @Test
    void refreshReturnsUnauthorizedForInvalidToken() throws Exception {
        when(authService.refresh(any(), eq("JUnit"), eq("10.0.0.12")))
                .thenThrow(new RefreshTokenInvalidException("Refresh token is invalid"));

        mockMvc.perform(post("/api/auth/refresh")
                        .header("User-Agent", "JUnit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"invalid-token\"}")
                        .with(request -> {
                            request.setRemoteAddr("10.0.0.12");
                            return request;
                        }))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_INVALID"))
                .andExpect(jsonPath("$.message").value("Refresh token is invalid"))
                .andExpect(jsonPath("$.traceId").isString());
    }

    @Test
    void refreshReturnsUnauthorizedForRevokedToken() throws Exception {
        when(authService.refresh(any(), eq("JUnit"), eq("10.0.0.12")))
                .thenThrow(new RefreshTokenReuseDetectedException("Refresh token reuse detected"));

        mockMvc.perform(post("/api/auth/refresh")
                        .header("User-Agent", "JUnit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"revoked-token\"}")
                        .with(request -> {
                            request.setRemoteAddr("10.0.0.12");
                            return request;
                        }))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_REUSED"))
                .andExpect(jsonPath("$.message").value("Refresh token reuse detected"))
                .andExpect(jsonPath("$.traceId").isString());
    }

    @Test
    void refreshReturnsValidationErrorForBlankToken() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .header("User-Agent", "JUnit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"\"}")
                        .with(request -> {
                            request.setRemoteAddr("10.0.0.12");
                            return request;
                        }))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Refresh token is required"))
                .andExpect(jsonPath("$.traceId").isString());
    }

    @Test
    void logoutRevokesSessionAndReturnsMessage() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"device-refresh-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out"));

        ArgumentCaptor<RefreshTokenRequest> captor = ArgumentCaptor.forClass(RefreshTokenRequest.class);
        verify(authService).logout(captor.capture());
        assertEquals("device-refresh-token", captor.getValue().refreshToken());
    }

    @Test
    void logoutReturnsUnauthorizedWhenNotAuthenticated() throws Exception {
        org.mockito.Mockito.doThrow(new AuthenticationCredentialsNotFoundException("Not authenticated"))
                .when(authService).logout(any());

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"device-refresh-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Not authenticated"))
                .andExpect(jsonPath("$.traceId").isString());
    }

    @Test
    void logoutAllRevokesAllSessions() throws Exception {
        mockMvc.perform(post("/api/auth/logout-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out from all sessions"));

        verify(authService).logoutAll();
    }

    @Test
    void sessionsReturnsActiveDeviceSessions() throws Exception {
        when(authService.getMySessions()).thenReturn(List.of(
                new SessionInfoResponse(
                        10L,
                        "fam-1",
                        "JUnit",
                        "10.0.0.1",
                        java.time.LocalDateTime.parse("2026-02-25T12:00:00"),
                        java.time.LocalDateTime.parse("2026-02-25T13:00:00"),
                        java.time.LocalDateTime.parse("2026-03-03T12:00:00")
                )
        ));

        mockMvc.perform(get("/api/auth/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].familyId").value("fam-1"))
                .andExpect(jsonPath("$[0].userAgent").value("JUnit"));
    }

    @Test
    void revokeSessionRevokesTargetSession() throws Exception {
        mockMvc.perform(delete("/api/auth/sessions/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Session revoked"));

        verify(authService).revokeMySession(10L);
    }

    @Test
    void resendVerificationEmailReturnsGenericSuccessMessage() throws Exception {
        mockMvc.perform(post("/api/auth/verify-email/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("If this account exists and is unverified, a verification email has been sent."));

        verify(authService).resendVerificationEmail("alice@example.com");
    }

    @ParameterizedTest
    @MethodSource("refreshErrorContractCases")
    void refreshErrorContractMatrix(Throwable error, int status, String code, String message) throws Exception {
        when(authService.refresh(any(), eq("JUnit"), eq("10.0.0.12")))
                .thenThrow(error);

        mockMvc.perform(post("/api/auth/refresh")
                        .header("User-Agent", "JUnit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"token-under-test\"}")
                        .with(request -> {
                            request.setRemoteAddr("10.0.0.12");
                            return request;
                        }))
                .andExpect(status().is(status))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.traceId").isString());
    }

    @Test
    void meReturnsUnauthorizedWhenNotAuthenticated() throws Exception {
        when(authService.getMe())
                .thenThrow(new AuthenticationCredentialsNotFoundException("Not authenticated"));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Not authenticated"))
                .andExpect(jsonPath("$.traceId").isString());
    }

    @Test
    void meReturnsForbiddenWhenAccessDenied() throws Exception {
        when(authService.getMe())
                .thenThrow(new AccessDeniedException("Denied"));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(jsonPath("$.traceId").isString());
    }

    @Test
    void verifyEmailReturnsBadRequestForMissingVerificationCode() throws Exception {
        org.mockito.Mockito.doThrow(new VerificationCodeNotFoundException("code not found"))
                .when(authService).verifyEmail("missing-code");

        mockMvc.perform(get("/api/auth/verify-email")
                        .param("token", "missing-code"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VERIFICATION_CODE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("code not found"))
                .andExpect(jsonPath("$.traceId").isString());
    }

    @Test
    void validateResetTokenReturnsBadRequestForMissingResetCode() throws Exception {
        when(authService.validateResetToken("missing-reset-code"))
                .thenThrow(new ResetPasswordTokenNotExistException("Token not found"));

        mockMvc.perform(get("/api/auth/forgot-password")
                        .param("code", "missing-reset-code"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RESET_PASSWORD_TOKEN_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Token not found"))
                .andExpect(jsonPath("$.traceId").isString());
    }

    private static Stream<Arguments> refreshErrorContractCases() {
        return Stream.of(
                Arguments.of(
                        new RefreshTokenExpiredException("Refresh token has expired"),
                        401,
                        "REFRESH_TOKEN_EXPIRED",
                        "Refresh token has expired"
                ),
                Arguments.of(
                        new RefreshTokenInvalidException("Refresh token is invalid"),
                        401,
                        "REFRESH_TOKEN_INVALID",
                        "Refresh token is invalid"
                ),
                Arguments.of(
                        new RefreshTokenReuseDetectedException("Refresh token reuse detected"),
                        401,
                        "REFRESH_TOKEN_REUSED",
                        "Refresh token reuse detected"
                ),
                Arguments.of(
                        new RefreshTokenOwnershipException("Refresh token does not belong to current user"),
                        403,
                        "REFRESH_TOKEN_FORBIDDEN",
                        "Refresh token does not belong to current user"
                ),
                Arguments.of(
                        new IllegalArgumentException("Refresh token is required"),
                        400,
                        "BAD_REQUEST",
                        "Refresh token is required"
                ),
                Arguments.of(
                        new IllegalStateException("Refresh token already rotated"),
                        400,
                        "BAD_REQUEST",
                        "Refresh token already rotated"
                ),
                Arguments.of(
                        new RuntimeException("Unexpected boom"),
                        500,
                        "INTERNAL_ERROR",
                        "Unexpected error"
                )
        );
    }
}
