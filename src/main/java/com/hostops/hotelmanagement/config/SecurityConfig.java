package com.hostops.hotelmanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import com.hostops.hotelmanagement.security.ApiAccessDeniedHandler;
import com.hostops.hotelmanagement.security.ApiAuthenticationEntryPoint;
import com.hostops.hotelmanagement.security.SesionRevocadaFilter;

/**
 * Configura la autenticación por sesión, CSRF y permisos de la API.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http,
			ApiAuthenticationEntryPoint authenticationEntryPoint,
			ApiAccessDeniedHandler accessDeniedHandler,
			SessionRegistry sessionRegistry) throws Exception {
		CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
		csrfHandler.setCsrfRequestAttributeName(null);

		http
				.csrf(csrf -> csrf
						.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
						.csrfTokenRequestHandler(csrfHandler))
				.authorizeHttpRequests(autorizacion -> autorizacion
						.requestMatchers("/", "/error", "/favicon.ico",
								"/css/**", "/js/**").permitAll()
						.requestMatchers("/api/auth/csrf", "/api/auth/login").permitAll()
						.requestMatchers("/api/roles/**", "/api/usuarios/**")
								.hasRole("ADMINISTRADOR")
						.requestMatchers(HttpMethod.POST, "/api/canales")
								.hasRole("ADMINISTRADOR")
						.requestMatchers(HttpMethod.PUT, "/api/canales/*")
								.hasRole("ADMINISTRADOR")
						.requestMatchers(HttpMethod.POST, "/api/habitaciones")
								.hasRole("ADMINISTRADOR")
						.requestMatchers(HttpMethod.PUT, "/api/habitaciones/*")
								.hasRole("ADMINISTRADOR")
						.requestMatchers(HttpMethod.PATCH, "/api/habitaciones/*/estado")
								.hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA")
						.requestMatchers(HttpMethod.POST, "/api/habitaciones/*/bloqueos")
								.hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA")
						.requestMatchers(HttpMethod.DELETE, "/api/bloqueos/*")
								.hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA")
						.requestMatchers(HttpMethod.POST, "/api/pagos/*/compensacion")
								.hasRole("ADMINISTRADOR")
						.requestMatchers("/api/**").authenticated()
						.anyRequest().permitAll())
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				.exceptionHandling(excepciones -> excepciones
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler));

		http.addFilterAfter(new SesionRevocadaFilter(sessionRegistry),
				SecurityContextHolderFilter.class);

		return http.build();
	}

	@Bean
	SessionRegistry sessionRegistry() {
		return new SessionRegistryImpl();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration configuracion)
			throws Exception {
		return configuracion.getAuthenticationManager();
	}

}
