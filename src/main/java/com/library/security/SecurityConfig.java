package com.library.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("librarian@library.com")
                        .password(encoder.encode("librarian"))
                        .roles("LIBRARIAN")
                        .build(),
                User.withUsername("member@library.com")
                        .password(encoder.encode("member"))
                        .roles("MEMBER")
                        .build(),
                User.withUsername("member2@library.com")
                        .password(encoder.encode("member2"))
                        .roles("MEMBER")
                        .build()
        );
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/books/**").hasAnyRole("LIBRARIAN", "MEMBER")
                        .requestMatchers("/api/books/**").hasRole("LIBRARIAN")
                        .requestMatchers("/api/members/**").hasRole("LIBRARIAN")
                        .requestMatchers(HttpMethod.POST, "/api/loans/borrow/**").hasRole("MEMBER")
                        .requestMatchers(HttpMethod.POST, "/api/loans/return/**").hasRole("MEMBER")
                        .requestMatchers(HttpMethod.GET, "/api/loans").hasAnyRole("LIBRARIAN", "MEMBER")
                        .requestMatchers("/api/loans/**").hasRole("LIBRARIAN")
                        .requestMatchers("/actuator/**").hasRole("LIBRARIAN")
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
