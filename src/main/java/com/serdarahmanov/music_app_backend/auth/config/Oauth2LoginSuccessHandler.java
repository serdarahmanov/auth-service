package com.serdarahmanov.music_app_backend.auth.config;

import com.serdarahmanov.music_app_backend.auth.forcodex.service.Oauth2AuthorizationCodeService;
import com.serdarahmanov.music_app_backend.auth.identity.Users;
import com.serdarahmanov.music_app_backend.auth.identity.repo.UserRepository;
import com.serdarahmanov.music_app_backend.auth.rolesAndPermissions.Role;
import com.serdarahmanov.music_app_backend.auth.rolesAndPermissions.RoleRepository;
import com.serdarahmanov.music_app_backend.utility.ApplicationProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class Oauth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final Oauth2AuthorizationCodeService authorizationCodeService;
    private final ApplicationProperties applicationProperties;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        if (!StringUtils.hasText(email)) {
            throw new IllegalStateException("OAuth2 provider did not return an email");
        }

        Users user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    Users u = new Users(
                            email,
                            generateUsername(email),
                            passwordEncoder.encode(UUID.randomUUID().toString())
                    );

                    u.setEnabled(true);
                    u.setPasswordSet(false);

                    Role userRole = roleRepository.findByName("ROLE_USER")
                            .orElseThrow(() -> new IllegalStateException("ROLE_USER not found"));

                    u.addRole(userRole);
                    return userRepository.save(u);
                });

        String code = authorizationCodeService.createCode(user.getUsername());

        String redirectUrl = UriComponentsBuilder
                .fromUriString(applicationProperties.getLoginSuccessUrl())
                .queryParam("code", code)
                .build(true)
                .toUriString();

        response.sendRedirect(redirectUrl);
    }

    private String generateUsername(String email) {
        return email.split("@")[0] + UUID.randomUUID().toString().substring(0, 5);
    }
}
