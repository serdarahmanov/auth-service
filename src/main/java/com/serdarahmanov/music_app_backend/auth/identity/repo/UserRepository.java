package com.serdarahmanov.music_app_backend.auth.identity.repo;

import com.serdarahmanov.music_app_backend.auth.identity.Users;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;

public interface UserRepository extends JpaRepository<Users,Long> {

    boolean existsByEmail(String email);

    Optional<Users> findByEmail(String email);


    Optional<Users> findByUsername(String username);

    boolean existsByUsername(@NotBlank(message = "Username is required") String username);

}
