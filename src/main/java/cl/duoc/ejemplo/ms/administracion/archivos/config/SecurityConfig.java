package cl.duoc.ejemplo.ms.administracion.archivos.config;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

/**
 * MODIFICADO
 *
 * Se agrega autorizacion basada en 2 roles, tal como pide el caso:
 * - ROLE_DESCARGAR_GUIAS: solo puede usar el endpoint de descarga (GET /s3/{bucket}/object)
 * - ROLE_GESTION_GUIAS: puede usar el resto de los endpoints (subir, mover, eliminar, listar)
 *
 * Azure AD B2C debe entregar estos roles en el claim "roles" del token (App Roles).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

		http.cors(Customizer.withDefaults())
				.authorizeHttpRequests(authorize -> authorize
						// Descargar guias: solo el rol especifico de descarga
						.requestMatchers(HttpMethod.GET, "/s3/*/object").hasRole("DESCARGAR_GUIAS")
						// Resto de endpoints de guias: el rol de gestion
						.requestMatchers("/s3/**").hasRole("GESTION_GUIAS")
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
		return http.build();
	}

	/**
	 * Convierte el claim "roles" del JWT de Azure AD B2C en GrantedAuthority
	 * con el prefijo ROLE_, que es lo que esperan hasRole(...) de arriba.
	 */
	private org.springframework.core.convert.converter.Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {

		return jwt -> {
			Collection<String> roles = jwt.getClaimAsStringList("roles");
			List<GrantedAuthority> authorities = (roles == null ? List.<String>of() : roles).stream()
					.map(role -> new SimpleGrantedAuthority("ROLE_" + role)).collect(Collectors.toList());
			return new JwtAuthenticationToken(jwt, authorities);
		};
	}
}
