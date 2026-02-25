package com.serdarahmanov.music_app_backend.users;

import com.serdarahmanov.music_app_backend.auth.rolesAndPermissions.Permission;
import com.serdarahmanov.music_app_backend.auth.verification.VerificationCode;
import com.serdarahmanov.music_app_backend.auth.forgotEmail.PasswordResetToken;
import com.serdarahmanov.music_app_backend.entity.AbstractEntity;
import com.serdarahmanov.music_app_backend.auth.rolesAndPermissions.Role;
import com.serdarahmanov.music_app_backend.users.dto.UpdateUserRequest;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor
public class Users extends AbstractEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String username;

    @Setter
    @Column(nullable = false)
    private String password;


//    it is for Oauth2 Login infrastructure
    @Setter
    @Column(nullable = false)
    private boolean passwordSet = false;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Setter
    @Column(nullable = false)
    private boolean enabled = false;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @Setter
    private String avatarKey;

    @Column(length = 500)
    private String bio;


    @Setter
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private VerificationCode verificationCode;

    @Setter
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PasswordResetToken> resetTokens = new ArrayList<>();


    public Users(String email,String username, String encodedPassword, String firstName, String lastName,
                 String avatar, String bio) {

        this.email = email;
        this.username = username;
        this.password = encodedPassword;
        this.firstName = firstName;
        this.lastName = lastName;
        this.avatarKey = avatar;
        this.bio = bio;
    }

    public void addRole(Role role) {
        roles.add(role);
    }

    public boolean hasRole(String roleName) {
        return roles.stream()
                .anyMatch(r -> r.getName().equals(roleName));
    }

    public void removeRole(Role role) {
        if ("ROLE_SUPER_ADMIN".equals(role.getName())) {
            throw new IllegalStateException("SUPER_ADMIN role cannot be removed");
        }
        this.roles.remove(role);
    }

    public void removeRoleByName(String roleName) {
        if ("ROLE_SUPER_ADMIN".equals(roleName)) {
            throw new IllegalStateException("SUPER_ADMIN role cannot be removed");
        }

        this.roles.removeIf(role -> role.getName().equals(roleName));
    }

    public Set<String> getRolesAsString(){
        Set<String> auths = new HashSet<>();

        for (Role role : roles) {
            auths.add(role.getName());

        }
        return  auths;
    }


    public void update(UpdateUserRequest request) {
        this.firstName= request.getFirstName();
        this.lastName= request.getLastName();
    }
}