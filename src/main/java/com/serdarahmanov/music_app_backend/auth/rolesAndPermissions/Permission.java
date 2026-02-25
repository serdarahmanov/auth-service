package com.serdarahmanov.music_app_backend.auth.rolesAndPermissions;

import com.serdarahmanov.music_app_backend.entity.AbstractEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "permissions")
@Getter
@Setter
public class Permission extends AbstractEntity {


    @Column(nullable = false, unique = true)
    private String name;
    // e.g. TRACK_EDIT_ALL, ALBUM_CREATE, USER_ASSIGN_ROLES

    @Column
    private String description;

    // getters & setters
}