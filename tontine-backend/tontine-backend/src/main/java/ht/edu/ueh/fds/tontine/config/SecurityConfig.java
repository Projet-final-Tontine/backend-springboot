package ht.edu.ueh.fds.tontine.config;

import ht.edu.ueh.fds.tontine.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuration centrale de la securite :
 * - mots de passe haches en BCrypt ;
 * - API sans session (stateless), authentifiee par jeton JWT ;
 * - endpoints publics (inscription, connexion, reset) ouverts ;
 * - endpoints /api/admin/** reserves au role ADMIN.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Sans jeton (ou jeton invalide) -> 401 Non authentifie.
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (req, res, e) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Non authentifie")))
                .authorizeHttpRequests(auth -> auth
                        // Endpoints publics : pas besoin d'etre connecte.
                        .requestMatchers("/api/auth/inscription", "/api/auth/connexion").permitAll()
                        .requestMatchers("/api/auth/google").permitAll()
                        .requestMatchers("/api/auth/mot-de-passe/**").permitAll()
                        // Disponibilite d'un username (verifiee pendant l'inscription).
                        .requestMatchers("/api/users/username-disponible").permitAll()
                        // Photos de profil televersees, lisibles sans jeton.
                        .requestMatchers("/uploads/**").permitAll()
                        // Verification publique d'un certificat de fiabilite (scan QR par une banque).
                        .requestMatchers("/api/releve/verifier/**").permitAll()
                        // Attestation publique d'integrite du Registre Inviolable.
                        .requestMatchers("/api/registre/integrite").permitAll()
                        // Pages web de la passerelle de paiement (ouvertes dans le navigateur).
                        .requestMatchers("/pay/**").permitAll()
                        // Page web d'administration (l'API interne reste protegee par jeton).
                        .requestMatchers("/admin.html", "/admin", "/").permitAll()
                        // Reserve a l'administrateur.
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Tout le reste exige un jeton valide.
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** Autorise le web et le mobile a appeler l'API depuis un autre domaine. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
