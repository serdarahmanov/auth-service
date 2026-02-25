package com.serdarahmanov.music_app_backend.auth.controller;

import com.serdarahmanov.music_app_backend.auth.config.Oauth2LoginSuccessHandler;
import com.serdarahmanov.music_app_backend.auth.jwt.JwtFilter;
import com.serdarahmanov.music_app_backend.auth.rolesAndPermissions.UserRoleService;
import com.serdarahmanov.music_app_backend.utility.config.AppSecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminUserRoleController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminUserRoleControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRoleService userRoleService;

    @MockitoBean
    private JwtFilter jwtFilter;

    @MockitoBean
    private Oauth2LoginSuccessHandler oauth2LoginSuccessHandler;

    @MockitoBean
    private AppSecurityProperties appSecurityProperties;

    @BeforeEach
    void setUp() {
        when(appSecurityProperties.getAllowedOrigins()).thenReturn(List.of("http://localhost:3000"));
    }

    @Test
    void getUserRolesReturnsRoles() throws Exception {
        when(userRoleService.getUserRoles(5L)).thenReturn(Set.of("ROLE_USER", "ROLE_ADMIN"));

        mockMvc.perform(get("/api/admin/users/5/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(5))
                .andExpect(jsonPath("$.roles").isArray());
    }

    @Test
    void addRoleAssignsRoleToUser() throws Exception {
        mockMvc.perform(post("/api/admin/users/7/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleName\":\"ROLE_EDITOR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Role assigned"));

        verify(userRoleService).addRoleToUser(7L, "ROLE_EDITOR");
    }

    @Test
    void removeRoleRevokesRoleFromUser() throws Exception {
        mockMvc.perform(delete("/api/admin/users/7/roles/ROLE_EDITOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Role revoked"));

        verify(userRoleService).removeRoleFromUser(7L, "ROLE_EDITOR");
    }

    @Test
    void replaceRolesReplacesRoleSet() throws Exception {
        mockMvc.perform(put("/api/admin/users/7/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\":[\"ROLE_USER\",\"ROLE_ADMIN\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Roles replaced"));

        verify(userRoleService).replaceUserRoles(7L, Set.of("ROLE_USER", "ROLE_ADMIN"));
    }
}
