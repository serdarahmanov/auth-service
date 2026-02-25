package com.serdarahmanov.music_app_backend.auth.rolesAndPermissions;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Set;

public interface RoleRepository extends JpaRepository< Role,Long> {
    Optional<Role> findByName(String name);

    // Used when replacing user roles in bulk
    Set<Role> findByNameIn(Set<String> names);


}
