package com.serdarahmanov.music_app_backend.auth.forcodex.service;

import com.serdarahmanov.music_app_backend.auth.dto.AuthTokenResponse;
import com.serdarahmanov.music_app_backend.auth.jwt.JWTService;
import com.serdarahmanov.music_app_backend.auth.refresh.RefreshTokenService;
import com.serdarahmanov.music_app_backend.auth.userDetails.MyUserDetailsService;
import com.serdarahmanov.music_app_backend.auth.identity.Users;
import com.serdarahmanov.music_app_backend.auth.identity.repo.UserRepository;
import com.serdarahmanov.music_app_backend.utility.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class Oauth2ExchangeService {

    private final Oauth2AuthorizationCodeService authorizationCodeService;
    private final MyUserDetailsService myUserDetailsService;
    private final JWTService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final JwtProperties jwtProperties;

    public AuthTokenResponse exchangeCodeForToken(String code, String userAgent, String ipAddress) {
        String username = authorizationCodeService.consumeUsername(code);
        UserDetails userDetails = myUserDetailsService.loadUserByUsername(username);
        Users user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = refreshTokenService.issue(user, userAgent, ipAddress);

        return new AuthTokenResponse(
                accessToken,
                "Bearer",
                jwtProperties.getExpiration(),
                refreshToken,
                jwtProperties.getRefreshExpiration()
        );
    }
}
