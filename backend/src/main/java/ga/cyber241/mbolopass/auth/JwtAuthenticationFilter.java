package ga.cyber241.mbolopass.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtService jwtService;
  private final UserDetailsService userDetailsService;

  public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
    this.jwtService = jwtService;
    this.userDetailsService = userDetailsService;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      try {
        String email = jwtService.subject(header.substring(7));
        UserDetails details = userDetailsService.loadUserByUsername(email);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(details.getUsername(), null, details.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
      } catch (RuntimeException ignored) {
        SecurityContextHolder.clearContext();
      }
    }
    chain.doFilter(request, response);
  }
}
