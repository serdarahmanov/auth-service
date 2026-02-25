package com.serdarahmanov.music_app_backend.auth.rolesAndPermissions;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission,Long> {
}
