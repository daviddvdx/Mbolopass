package ga.cyber241.mbolopass.auth;

import ga.cyber241.mbolopass.user.User;
import ga.cyber241.mbolopass.user.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
  @Bean
  SecurityFilterChain security(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
    return http.csrf(csrf -> csrf.disable())
        .cors(cors -> {})
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint((request, response, exception) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
            .accessDeniedHandler((request, response, exception) -> response.sendError(HttpServletResponse.SC_FORBIDDEN)))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health").permitAll()
            .requestMatchers("/api/v1/auth/**").permitAll()
            .requestMatchers("/api/v1/public/**").permitAll()
            .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.GET, "/api/v1/profile/me").authenticated()
            .requestMatchers("/api/v1/health-profile/**").hasRole("PATIENT")
            .requestMatchers("/api/v1/card/**").hasRole("PATIENT")
            .requestMatchers("/api/v1/prevention/**").hasRole("PATIENT")
            .requestMatchers("/api/v1/ai-summary/**").hasRole("PATIENT")
            .requestMatchers("/api/v1/dependents/**").hasRole("PATIENT")
            .requestMatchers("/api/v1/documents/**").hasRole("PATIENT")
            .anyRequest().authenticated())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  @Bean
  UserDetailsService userDetailsService(UserRepository users) {
    return email -> {
      User user = users.findByEmail(email.toLowerCase()).orElseThrow();
      return org.springframework.security.core.userdetails.User
          .withUsername(user.getEmail())
          .password(user.getPasswordHash())
          .authorities("ROLE_" + user.getRole().name())
          .disabled(!user.isEnabled())
          .build();
    };
  }

  @Bean
  AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
    return configuration.getAuthenticationManager();
  }

  @Bean
  PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

  @Bean
  CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins}") String origins) {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of(origins.split(",")));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  static UsernamePasswordAuthenticationToken authToken(String email) {
    return new UsernamePasswordAuthenticationToken(email, null, List.of());
  }
}
