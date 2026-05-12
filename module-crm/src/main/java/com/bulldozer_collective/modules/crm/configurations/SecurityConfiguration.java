package com.bulldozer_collective.modules.crm.configurations;

import com.bulldozer_collective.security.services.KeycloakJwtTokenConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.DelegatingJwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    @Primary
    @Profile("dev")
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Collections.singletonList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Collections.singletonList("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    @Order(2)
    SecurityFilterChain resourceServerSecurityFilterChain(HttpSecurity http,
                                                          CorsConfigurationSource cors) throws Exception {
        var authoritiesConverter = new DelegatingJwtGrantedAuthoritiesConverter(
                new JwtGrantedAuthoritiesConverter(),
                new KeycloakJwtTokenConverter());

        http
                .securityMatcher("/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(c -> c.configurationSource(cors))
                .anonymous(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(r -> r.anyRequest().authenticated())
                .oauth2ResourceServer(r -> r.jwt(jwt -> jwt
                        .jwtAuthenticationConverter(j -> {
                            var authorities = authoritiesConverter.convert(j);
                            return new JwtAuthenticationToken(j, authorities);
                        })))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    @Bean
    @Order(1)
    SecurityFilterChain internalFilterChain(HttpSecurity http,
                                            CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .securityMatcher("/internal/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(c -> c.configurationSource(corsConfigurationSource))
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }
}
