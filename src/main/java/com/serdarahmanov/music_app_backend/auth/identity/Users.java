package com.serdarahmanov.music_app_backend.auth.identity;

import com.serdarahmanov.music_app_backend.auth.verification.VerificationCode;
import com.serdarahmanov.music_app_backend.auth.forgotEmail.PasswordResetToken;
import com.serdarahmanov.music_app_backend.entity.AbstractEntity;
import com.serdarahmanov.music_app_backend.auth.rolesAndPermissions.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "auth_users")
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
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private VerificationCode verificationCode;

    @Setter
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PasswordResetToken> resetTokens = new ArrayList<>();


    public Users(String email, String username, String encodedPassword) {

        this.email = email;
        this.username = username;
        this.password = encodedPassword;
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
}
