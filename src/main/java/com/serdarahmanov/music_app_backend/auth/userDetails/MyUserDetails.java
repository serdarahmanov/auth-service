package com.serdarahmanov.music_app_backend.auth.userDetails;

import com.serdarahmanov.music_app_backend.users.Users;
import com.serdarahmanov.music_app_backend.auth.rolesAndPermissions.Permission;
import com.serdarahmanov.music_app_backend.auth.rolesAndPermissions.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


public class MyUserDetails implements UserDetails {

    private final Users user;
    private final Set<GrantedAuthority> authorities;

    public MyUserDetails(Users user) {
        this.user = user;
        this.authorities = buildAuthorities(user);
    }

    private Set<GrantedAuthority> buildAuthorities(Users user) {
        Set<GrantedAuthority> auths = new HashSet<>();

        for (Role role : user.getRoles()) {
            auths.add(new SimpleGrantedAuthority(role.getName()));

            for (Permission perm : role.getPermissions()) {
                auths.add(new SimpleGrantedAuthority(perm.getName()));
            }
        }
        return auths;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return user.isEnabled(); }


    public Long getId() {
        return user.getId();
    }

    public String getEmail() {
        return user.getEmail();
    }


    public Set<String> getAuthoritiesAsStrings() {
        Set<String> auths = new HashSet<>();

        for (Role role : user.getRoles()) {
            auths.add(role.getName());

            for (Permission perm : role.getPermissions()) {
                auths.add(perm.getName());
            }
        }
        return auths;
    }

    public boolean isPasswordSet() {
        return user.isPasswordSet();
    }

    public String getFirstName(){
        return user.getFirstName();
    }

    public String getLastName(){
        return user.getLastName();
    }
}
