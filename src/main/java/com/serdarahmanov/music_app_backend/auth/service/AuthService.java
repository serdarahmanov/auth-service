package com.serdarahmanov.music_app_backend.auth.service;

import com.serdarahmanov.music_app_backend.auth.dto.AuthMeResponse;
import com.serdarahmanov.music_app_backend.auth.dto.AuthTokenResponse;
import com.serdarahmanov.music_app_backend.auth.dto.CreateUserRequest;
import com.serdarahmanov.music_app_backend.auth.dto.RefreshTokenRequest;
import com.serdarahmanov.music_app_backend.auth.dto.SessionInfoResponse;
import com.serdarahmanov.music_app_backend.auth.dto.UserResponse;
import com.serdarahmanov.music_app_backend.auth.forgotEmail.PasswordResetToken;
import com.serdarahmanov.music_app_backend.auth.forgotEmail.PasswordResetTokenRepo;
import com.serdarahmanov.music_app_backend.auth.jwt.JWTService;
import com.serdarahmanov.music_app_backend.auth.refresh.RefreshRotationResult;
import com.serdarahmanov.music_app_backend.auth.refresh.RefreshTokenService;
import com.serdarahmanov.music_app_backend.auth.security.LoginAttemptService;
import com.serdarahmanov.music_app_backend.auth.dto.UserNameAndPassword;
import com.serdarahmanov.music_app_backend.auth.rolesAndPermissions.Role;
import com.serdarahmanov.music_app_backend.auth.rolesAndPermissions.RoleRepository;
import com.serdarahmanov.music_app_backend.auth.userDetails.MyUserDetails;
import com.serdarahmanov.music_app_backend.auth.verification.VerificationCode;
import com.serdarahmanov.music_app_backend.auth.jobs.resetEmailJob.SendResetPasswordEmailJob;
import com.serdarahmanov.music_app_backend.auth.jobs.welcomeEmailJob.SendWelcomeEmailJob;
import com.serdarahmanov.music_app_backend.auth.verification.VerificationRepo;
import com.serdarahmanov.music_app_backend.users.Users;
import com.serdarahmanov.music_app_backend.users.repo.UserRepository;
import com.serdarahmanov.music_app_backend.utility.customExceptions.ResetPasswordTokenNotExistException;
import com.serdarahmanov.music_app_backend.utility.customExceptions.VerificationCodeNotFoundException;
import com.serdarahmanov.music_app_backend.utility.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.jobrunr.scheduling.BackgroundJobRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;
   private final UserRepository userRepository;
   private final VerificationRepo verificationRepo;
   private final PasswordResetTokenRepo passwordResetTokenRepo;
   private final PasswordEncoder passwordEncoder;
   private final RoleRepository roleRepository;
   private final RefreshTokenService refreshTokenService;
   private final JwtProperties jwtProperties;
   private final LoginAttemptService loginAttemptService;


    public AuthTokenResponse login(UserNameAndPassword userNameAndPassword, String userAgent, String ipAddress) {
        String username = userNameAndPassword.getUsername();
        loginAttemptService.assertNotBlocked(username, ipAddress);

        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            username,
                            userNameAndPassword.getPassword()
                    )
            );
        } catch (BadCredentialsException ex) {
            loginAttemptService.recordFailedAttempt(username, ipAddress);
            throw ex;
        }



        if (!auth.isAuthenticated()) {
            loginAttemptService.recordFailedAttempt(username, ipAddress);
            throw new BadCredentialsException("Invalid credentials");
        }

        loginAttemptService.recordSuccessfulLogin(username, ipAddress);


        UserDetails userDetails = (UserDetails) auth.getPrincipal();

        Users user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return issueTokenPair(user, userDetails, userAgent, ipAddress);
    }

    public AuthTokenResponse refresh(RefreshTokenRequest request, String userAgent, String ipAddress) {
        RefreshRotationResult rotationResult = refreshTokenService.rotate(
                request.refreshToken(),
                userAgent,
                ipAddress
        );

        UserDetails userDetails = new MyUserDetails(
                userRepository.findByUsername(rotationResult.username())
                        .orElseThrow(() -> new UsernameNotFoundException("User not found"))
        );

        String accessToken = jwtService.generateToken(userDetails);
        return new AuthTokenResponse(
                accessToken,
                "Bearer",
                jwtProperties.getExpiration(),
                rotationResult.refreshToken(),
                jwtProperties.getRefreshExpiration()
        );
    }

    public void logout(RefreshTokenRequest request) {
        Users user = getAuthenticatedUser();
        refreshTokenService.revokeForUser(user.getId(), request.refreshToken());
    }

    public void logoutAll() {
        Users user = getAuthenticatedUser();
        refreshTokenService.revokeAllForUser(user.getId());
    }

    public List<SessionInfoResponse> getMySessions() {
        Users user = getAuthenticatedUser();
        return refreshTokenService.listActiveSessionsForUser(user.getId());
    }

    public void revokeMySession(Long sessionId) {
        Users user = getAuthenticatedUser();
        refreshTokenService.revokeSessionForUser(user.getId(), sessionId);
    }



    public AuthMeResponse getMe() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication.getPrincipal().equals("anonymousUser")) {
            throw new AuthenticationCredentialsNotFoundException("Not authenticated");
        }

        MyUserDetails user = (MyUserDetails) authentication.getPrincipal();

        return new AuthMeResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.isEnabled(),
                user.isPasswordSet(),
                user.getAuthoritiesAsStrings()
        );
    }

    @Transactional
    public void handleForgotPassword(String email) {
        Users user = userRepository.findByEmail(email).orElse(null);

        // Always return success to avoid email enumeration
        if (user == null) {
            return;
        }


        // if the user has come from Oauth2 Google
        if (!user.isPasswordSet()) {
            return; // silently ignore (prevents abuse)
        }
        

        PasswordResetToken passwordResetToken = new PasswordResetToken(user);
        passwordResetTokenRepo.save(passwordResetToken);
        SendResetPasswordEmailJob sendResetPasswordEmailJob = new SendResetPasswordEmailJob(user.getId());
        BackgroundJobRequest.enqueue(sendResetPasswordEmailJob);

    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already in use");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        Users user = new Users(
                request.getEmail(),
                request.getUsername(),
                encodedPassword,
                request.getFirstName(),
                request.getLastName(),
                request.getAvatar(),
                request.getBio()
        );
        user.setPasswordSet(true);


        Role defaultRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new IllegalStateException("ROLE_USER not found"));

        user.addRole(defaultRole);


        userRepository.save(user);

        sendVerificationEmail(user);

        return UserResponse.fromEntity(user);
    }

    private void sendVerificationEmail(Users user) {
        VerificationCode verificationCode = new VerificationCode(user);
        verificationRepo.save(verificationCode);
        SendWelcomeEmailJob sendWelcomeEmailJob = new SendWelcomeEmailJob(user.getId());
        BackgroundJobRequest.enqueue(sendWelcomeEmailJob);
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        Users user = userRepository.findByEmail(email).orElse(null);

        // Keep response generic to prevent account enumeration.
        if (user == null || user.isEnabled()) {
            return;
        }

        verificationRepo.findByUserId(user.getId()).ifPresent(verificationRepo::delete);
        sendVerificationEmail(user);
    }


    @Transactional
    public void verifyEmail(String code) {
        VerificationCode verificationCode = verificationRepo.findByCode(code).orElseThrow( ()-> new VerificationCodeNotFoundException("code not found"));

        if (verificationCode.isUsed()) {
            throw new IllegalStateException("Verification code has already been used");
        }
        if (verificationCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Verification code has expired");
        }

        // Mark user as verified
        Users user = verificationCode.getUser();
        user.setEnabled(true);
        userRepository.save(user);

        // Mark verification code as used
        verificationCode.setUsed(true);
        verificationRepo.delete(verificationCode);
    }



    @Transactional
    public PasswordResetToken validateResetToken(String code) {
        PasswordResetToken token = passwordResetTokenRepo.findByCode(code).orElseThrow(()-> new ResetPasswordTokenNotExistException("Token not found"));


        if(token.isUsed()){
            passwordResetTokenRepo.delete(token);
            throw new IllegalStateException("The Reset Token code has already been used");

        }

        if(token.isExpired()){
            passwordResetTokenRepo.delete(token);
            throw new IllegalStateException("The Reset Token code has been expired");
        }

        return token;

    }


    @Transactional
    public void resetPassword(String code, String newPassword) {

        PasswordResetToken token = validateResetToken(code);

        Users user = token.getUser();

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordSet(true); // 🔴 IMPORTANT
        token.setUsed(true);

        userRepository.save(user);
        passwordResetTokenRepo.delete(token);
    }

    public Users getAuthenticatedUser() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication.getPrincipal().equals("anonymousUser")) {
            throw new AuthenticationCredentialsNotFoundException("Not authenticated");
        }

        MyUserDetails user = (MyUserDetails) authentication.getPrincipal();

        return userRepository.findById(user.getId()).orElseThrow(()-> new UsernameNotFoundException("User not found"));
    }

    @Transactional
    public void requestPasswordSet() {
        Users user = getAuthenticatedUser();

        if (user.isPasswordSet()) {
            throw new IllegalStateException("Password already set");
        }

        VerificationCode code = new VerificationCode(user);
        verificationRepo.save(code);

        BackgroundJobRequest.enqueue(
                new SendResetPasswordEmailJob(user.getId())
        );
    }


    @Transactional
    public void updatePasswordWithOldPassword( String oldPassword, String password) {

        Users user = getAuthenticatedUser();

        if(!passwordEncoder.matches(oldPassword, user.getPassword())){

            throw new IllegalStateException("Your old password does not match");
        }


        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);

    }

    private AuthTokenResponse issueTokenPair(
            Users user,
            UserDetails userDetails,
            String userAgent,
            String ipAddress
    ) {
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
