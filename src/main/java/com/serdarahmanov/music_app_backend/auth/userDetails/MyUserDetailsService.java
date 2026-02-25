package com.serdarahmanov.music_app_backend.auth.userDetails;

import com.serdarahmanov.music_app_backend.users.Users;
import com.serdarahmanov.music_app_backend.users.repo.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class MyUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public MyUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Users user = userRepository.findByUsername(username).orElseThrow(() ->
                        new UsernameNotFoundException("User not found: " + username));



        return new MyUserDetails(user); // wrap your entity
    }
}