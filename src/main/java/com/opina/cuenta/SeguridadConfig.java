package com.opina.cuenta;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration(proxyBeanMethods = false)
public class SeguridadConfig {

    @Bean
    SecurityFilterChain seguridad(HttpSecurity http, AuthenticationEntryPoint sinSesion) throws Exception {
        http
                // API sin cookies: el token viaja en la cabecera Authorization, así que CSRF no aplica
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rutas -> rutas
                        // Lo que usa el cliente que opina desde la tarjeta o el QR, sin cuenta
                        .requestMatchers(HttpMethod.GET, "/api/negocios/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/negocios/*/opiniones").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/registro", "/api/auth/login").permitAll()
                        .requestMatchers("/docs/**", "/swagger-ui/**", "/v3/api-docs/**", "/actuator/health", "/error").permitAll()
                        // Todo lo demás es del panel del negocio
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()).authenticationEntryPoint(sinSesion))
                .exceptionHandling(e -> e.authenticationEntryPoint(sinSesion));
        return http.build();
    }

    /**
     * Sin token o con uno caducado o falso: se responde con el mismo formato de error que el resto de la API.
     * Spring Security corre antes que los controladores, así que el error se pasa al manejador de errores de MVC.
     */
    @Bean
    AuthenticationEntryPoint sinSesion(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        return (peticion, respuesta, error) -> {
            respuesta.setHeader("WWW-Authenticate", "Bearer");
            resolver.resolveException(peticion, respuesta, null, error);
            if (!respuesta.isCommitted() && respuesta.getStatus() < 400) {
                respuesta.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            }
        };
    }

    /** BCrypt con el prefijo {bcrypt}: si un día se cambia de algoritmo, los hashes antiguos siguen valiendo. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
