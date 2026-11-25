package com.backlog.service.config;

import com.backlog.service.security.CustomPasswordEncoder;
import com.backlog.service.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración central de seguridad del servicio de backlog.
 * Define la cadena de filtros, la política de sesión sin estado y
 * los endpoints públicos de registro y autenticación.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomPasswordEncoder customPasswordEncoder;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CustomPasswordEncoder customPasswordEncoder) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.customPasswordEncoder = customPasswordEncoder;
    }

    /**
     * Define la cadena de filtros de Spring Security.
     *
     * @param http configuración HTTP de Spring Security
     * @return la cadena de filtros configurada
     * @throws Exception si ocurre un error al construir la cadena
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Habilita la configuración CORS definida para el frontend Angular.
                .cors(Customizer.withDefaults())
                // API sin estado: se deshabilita CSRF y no se usan sesiones.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // Endpoints públicos de registro y autenticación.
                        .requestMatchers("/api/auth/**", "/api/registro/**").permitAll()
                        // Permite las peticiones de verificación CORS (preflight).
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // El resto requiere autenticación mediante JWT.
                        .anyRequest().authenticated())
                // Registra el filtro JWT antes del filtro de autenticación por formulario.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Expone el codificador de contraseñas personalizado como bean de Spring Security.
     *
     * @return codificador de contraseñas
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return customPasswordEncoder;
    }

    /**
     * Define las reglas CORS para permitir el consumo de la API desde Angular.
     *
     * @return configuración CORS aplicada a todos los endpoints
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Orígenes permitidos del frontend en desarrollo.
        configuration.setAllowedOrigins(List.of("http://localhost:4200", "http://127.0.0.1:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Expone el gestor de autenticación para el módulo de login.
     *
     * @param authenticationConfiguration configuración de autenticación de Spring
     * @return gestor de autenticación
     * @throws Exception si no se puede obtener el gestor
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
