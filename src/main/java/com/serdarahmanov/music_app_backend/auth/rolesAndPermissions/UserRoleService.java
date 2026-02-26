package com.serdarahmanov.music_app_backend.auth.rolesAndPermissions;

import com.serdarahmanov.music_app_backend.auth.identity.Users;
import com.serdarahmanov.music_app_backend.auth.identity.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserRoleService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserRoleService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    /* =========================
       ADD ROLE TO USER
       ========================= */
    public void addRoleToUser(Long userId, String roleName) {
        String normalizedRoleName = normalizeRoleName(roleName);

        Users user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));

        Role role = roleRepository.findByName(normalizedRoleName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        // Avoid duplicates
        if (user.hasRole(normalizedRoleName)) {
            return; // idempotent
        }

        user.addRole(role);
    }

    /* =========================
       REMOVE ROLE FROM USER
       ========================= */
    public void removeRoleFromUser(Long userId, String roleName) {
        String normalizedRoleName = normalizeRoleName(roleName);

        if ("ROLE_SUPER_ADMIN".equals(normalizedRoleName)) {
            throw new IllegalStateException("SUPER_ADMIN role cannot be removed");
        }

        Users user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.removeRoleByName(normalizedRoleName);
    }

    /* =========================
       REPLACE USER ROLES (ADMIN USE)
       ========================= */
    public void replaceUserRoles(Long userId, Set<String> roleNames) {

        Users user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.hasRole("ROLE_SUPER_ADMIN")) {
            throw new IllegalStateException("SUPER_ADMIN roles cannot be replaced");
        }

        Set<String> normalizedRoleNames = normalizeRoleNames(roleNames);
        Set<Role> newRoles = roleRepository.findByNameIn(normalizedRoleNames);
        Set<String> foundRoleNames = newRoles.stream().map(Role::getName).collect(Collectors.toSet());
        Set<String> missingRoleNames = normalizedRoleNames.stream()
                .filter(roleName -> !foundRoleNames.contains(roleName))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!missingRoleNames.isEmpty()) {
            throw new IllegalArgumentException("Unknown roles: " + String.join(", ", missingRoleNames));
        }

        user.getRoles().clear();
        user.getRoles().addAll(newRoles);
    }

    /* =========================
       CHECK USER ROLE
       ========================= */
    @Transactional(readOnly = true)
    public boolean userHasRole(Long userId, String roleName) {

        return userRepository.findById(userId).map(user -> user.hasRole(roleName)).orElse(false);
    }

    /* =========================
       GET USER ROLES
       ========================= */
    @Transactional(readOnly = true)
    public Set<String> getUserRoles(Long userId) {

        Users user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));

        return user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
    }

    private static String normalizeRoleName(String roleName) {
        if (!StringUtils.hasText(roleName)) {
            throw new IllegalArgumentException("Role name is required");
        }
        return roleName.trim().toUpperCase(Locale.ROOT);
    }

    private static Set<String> normalizeRoleNames(Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            throw new IllegalArgumentException("Roles are required");
        }
        Set<String> normalized = roleNames.stream()
                .map(UserRoleService::normalizeRoleName)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Roles are required");
        }
        return normalized;
    }
}
