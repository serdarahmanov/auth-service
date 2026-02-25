package com.serdarahmanov.music_app_backend.auth.rolesAndPermissions;

import com.serdarahmanov.music_app_backend.entity.AbstractEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
@Getter
@Setter
public class Role extends AbstractEntity {

    @Column(nullable = false, unique = true)
    private String name;
    // ROLE_USER, ROLE_ARTIST, ROLE_EDITOR, ROLE_ADMIN, ROLE_SUPER_ADMIN

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();

}
