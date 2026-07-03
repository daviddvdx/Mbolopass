package ga.cyber241.mbolopass.auth;

import ga.cyber241.mbolopass.common.Enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

record RegisterRequest(@NotBlank String firstName, @NotBlank String lastName, @Email @NotBlank String email, @Size(min = 8) String password) {}
record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}
record AuthResponse(String accessToken, String tokenType, long expiresInMinutes, MeResponse user) {}
record MeResponse(UUID id, String firstName, String lastName, String email, Role role, Set<String> roles) {}
