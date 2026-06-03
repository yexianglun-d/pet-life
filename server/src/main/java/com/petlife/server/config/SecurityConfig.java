package com.petlife.server.config;

import com.petlife.server.modules.auth.security.DevelopmentTokenAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * 当前阶段安全配置。
 *
 * <p>Phase 1 先聚焦接口契约和主链路开发，认证能力通过业务接口自行演进，
 * 因此这里先关闭默认表单登录和 Basic Auth，避免 Spring Security 的默认行为阻塞联调。
 * 后续接入 JWT 时，在该配置上继续收紧即可。</p>
 */
@Configuration
public class SecurityConfig {

    private final String corsAllowedOriginPatterns;

    public SecurityConfig(
        @Value("${petlife.cors.allowed-origin-patterns:http://localhost:5173,http://127.0.0.1:5173,https://pet.howied.me}")
        String corsAllowedOriginPatterns
    ) {
        this.corsAllowedOriginPatterns = corsAllowedOriginPatterns;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity httpSecurity,
        DevelopmentTokenAuthenticationFilter developmentTokenAuthenticationFilter
    ) throws Exception {
        httpSecurity
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(developmentTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());

        return httpSecurity.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowedOriginPatterns(parseCorsAllowedOriginPatterns());
        corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        corsConfiguration.setAllowedHeaders(List.of("*"));
        corsConfiguration.setExposedHeaders(List.of("Authorization"));
        corsConfiguration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }

    private List<String> parseCorsAllowedOriginPatterns() {
        return Arrays.stream(corsAllowedOriginPatterns.split(","))
            .map(String::trim)
            .filter(originPattern -> !originPattern.isBlank())
            .toList();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
