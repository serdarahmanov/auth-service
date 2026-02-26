package com.serdarahmanov.music_app_backend.auth.service;

import com.serdarahmanov.music_app_backend.auth.dto.AuthTokenResponse;
import com.serdarahmanov.music_app_backend.auth.dto.RefreshTokenRequest;
import com.serdarahmanov.music_app_backend.auth.dto.UserNameAndPassword;
import com.serdarahmanov.music_app_backend.auth.forgotEmail.PasswordResetTokenRepo;
import com.serdarahmanov.music_app_backend.auth.jwt.JWTService;
import com.serdarahmanov.music_app_backend.auth.refresh.RefreshTokenService;
import com.serdarahmanov.music_app_backend.auth.rolesAndPermissions.RoleRepository;
import com.serdarahmanov.music_app_backend.auth.security.LoginAttemptService;
import com.serdarahmanov.music_app_backend.auth.userDetails.MyUserDetails;
import com.serdarahmanov.music_app_backend.auth.verification.VerificationRepo;
import com.serdarahmanov.music_app_backend.entity.AbstractEntity;
import com.serdarahmanov.music_app_backend.auth.identity.Users;
import com.serdarahmanov.music_app_backend.auth.identity.repo.UserRepository;
import com.serdarahmanov.music_app_backend.utility.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JWTService jwtService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private VerificationRepo verificationRepo;
    @Mock
    private PasswordResetTokenRepo passwordResetTokenRepo;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private LoginAttemptService loginAttemptService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setExpiration(3_600_000L);
        jwtProperties.setRefreshExpiration(1_209_600_000L);

        authService = new AuthService(
                authenticationManager,
                jwtService,
                userRepository,
                verificationRepo,
                passwordResetTokenRepo,
                passwordEncoder,
                roleRepository,
                refreshTokenService,
                jwtProperties,
                loginAttemptService
        );
    }

    @Test
    void loginReturnsAccessAndRefreshTokens() {
        User principal = new User("alice", "secret", List.of());
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(authentication);

        Users user = new Users("alice@example.com", "alice", "encoded");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(principal)).thenReturn("access-token");
        when(refreshTokenService.issue(user, "ua", "127.0.0.1")).thenReturn("refresh-token");

        AuthTokenResponse response = authService.login(
                new UserNameAndPassword("alice", "secret"),
                "ua",
                "127.0.0.1"
        );

        assertEquals("access-token", response.accessToken());
        assertEquals("Bearer", response.accessTokenType());
        assertEquals("refresh-token", response.refreshToken());
        verify(loginAttemptService).assertNotBlocked("alice", "127.0.0.1");
        verify(loginAttemptService).recordSuccessfulLogin("alice", "127.0.0.1");
    }

    @Test
    void logoutRevokesProvidedRefreshTokenFamilyForUser() {
        Users user = new Users("alice@example.com", "alice", "encoded");
        setId(user, 42L);

        MyUserDetails principal = new MyUserDetails(user);
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(userRepository.findById(42L)).thenReturn(Optional.of(user));

        authService.logout(new RefreshTokenRequest("device-refresh-token"));

        verify(refreshTokenService).revokeForUser(42L, "device-refresh-token");
        SecurityContextHolder.clearContext();
    }

    @Test
    void loginRecordsFailedAttemptWhenCredentialsAreInvalid() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(
                new UserNameAndPassword("alice", "wrong"),
                "ua",
                "127.0.0.1"
        ));

        verify(loginAttemptService).assertNotBlocked("alice", "127.0.0.1");
        verify(loginAttemptService).recordFailedAttempt("alice", "127.0.0.1");
    }

    private static void setId(AbstractEntity entity, long value) {
        try {
            Field idField = AbstractEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, value);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }
}
