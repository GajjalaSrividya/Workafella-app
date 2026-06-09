package com.workafella.config;

import com.workafella.auth.AppUser;
import com.workafella.auth.JwtService;
import com.workafella.auth.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtFilter jwtFilter) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration cfg = new CorsConfiguration();
                    cfg.setAllowedOrigins(List.of(
                            "http://localhost:5173", "http://127.0.0.1:5173",
                            "http://localhost:5174", "http://127.0.0.1:5174","http://localhost:5175",
    "http://127.0.0.1:5175"));
                    cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                    cfg.setAllowedHeaders(List.of("*"));
                    return cfg;
                }))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    JwtFilter jwtFilter(JwtService jwtService, UserRepository users) {
        return new JwtFilter(jwtService, users);
    }

    static class JwtFilter extends OncePerRequestFilter {
        private final JwtService jwtService;
        private final UserRepository users;

        JwtFilter(JwtService jwtService, UserRepository users) {
            this.jwtService = jwtService;
            this.users = users;
        }

      @Override
protected void doFilterInternal(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain chain)
        throws ServletException, IOException {

    System.out.println("REQUEST = " + request.getRequestURI());

    String header = request.getHeader(HttpHeaders.AUTHORIZATION);

    System.out.println("AUTH HEADER = " + header);

    if (header != null && header.startsWith("Bearer ")) {

        String email = jwtService.subject(header.substring(7));

        System.out.println("EMAIL FROM JWT = " + email);

        AppUser user = users.findByEmail(email).orElse(null);

        System.out.println("USER FOUND = " + (user != null));

        if (user != null) {
            System.out.println("ROLE = " + user.getRole());
            System.out.println("ENABLED = " + user.isEnabled());
            System.out.println("COMPANY = " +
                    (user.getCompany() == null ? "NULL" : user.getCompany().getId()));
        }

        if (user != null && user.isEnabled()) {

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            List.of(
                                    new SimpleGrantedAuthority(
                                            "ROLE_" + user.getRole().name()
                                    )
                            )
                    );

            SecurityContextHolder.getContext().setAuthentication(auth);

            System.out.println("AUTHENTICATION SET");
        }
    }

    chain.doFilter(request, response);
}
    }
}
