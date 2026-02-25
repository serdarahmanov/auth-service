package com.serdarahmanov.music_app_backend.auth.controller;

import com.serdarahmanov.music_app_backend.auth.dto.AdminReplaceUserRolesRequest;
import com.serdarahmanov.music_app_backend.auth.dto.AdminRoleAssignmentRequest;
import com.serdarahmanov.music_app_backend.auth.dto.AdminUserRolesResponse;
import com.serdarahmanov.music_app_backend.auth.rolesAndPermissions.UserRoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserRoleController {

    private final UserRoleService userRoleService;

    @GetMapping("/{userId}/roles")
    @PreAuthorize("hasAnyAuthority('USER_ASSIGN_ROLES', 'USER_REVOKE_ROLES')")
    public ResponseEntity<AdminUserRolesResponse> getUserRoles(@PathVariable Long userId) {
        Set<String> roles = userRoleService.getUserRoles(userId);
        return ResponseEntity.ok(new AdminUserRolesResponse(userId, roles));
    }

    @PostMapping("/{userId}/roles")
    @PreAuthorize("hasAuthority('USER_ASSIGN_ROLES')")
    public ResponseEntity<?> addRoleToUser(
            @PathVariable Long userId,
            @Valid @RequestBody AdminRoleAssignmentRequest request
    ) {
        userRoleService.addRoleToUser(userId, request.roleName());
        return ResponseEntity.ok(Map.of("message", "Role assigned"));
    }

    @DeleteMapping("/{userId}/roles/{roleName}")
    @PreAuthorize("hasAuthority('USER_REVOKE_ROLES')")
    public ResponseEntity<?> removeRoleFromUser(
            @PathVariable Long userId,
            @PathVariable String roleName
    ) {
        userRoleService.removeRoleFromUser(userId, roleName);
        return ResponseEntity.ok(Map.of("message", "Role revoked"));
    }

    @PutMapping("/{userId}/roles")
    @PreAuthorize("hasAuthority('USER_ASSIGN_ROLES')")
    public ResponseEntity<?> replaceUserRoles(
            @PathVariable Long userId,
            @Valid @RequestBody AdminReplaceUserRolesRequest request
    ) {
        userRoleService.replaceUserRoles(userId, request.roles());
        return ResponseEntity.ok(Map.of("message", "Roles replaced"));
    }
}
