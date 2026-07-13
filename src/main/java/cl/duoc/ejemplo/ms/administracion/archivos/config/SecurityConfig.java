package cl.duoc.ejemplo.ms.administracion.archivos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * MODIFICADO (TEMPORAL)
 *
 * Se desactiva la restriccion por rol (hasRole(...)) mientras se termina de
 * configurar Azure AD B2C App Roles. Se mantiene la exigencia de JWT valido
 * (authenticated()), o sea que sigue siendo necesario un token de Azure AD,
 * solo que ya no importa si trae o no el claim "roles".
 *
 * IMPORTANTE: revertir a la version con hasRole("DESCARGAR_GUIAS") /
 * hasRole("GESTION_GUIAS") antes de la entrega/demo, porque el caso pide
 * explicitamente probar la restriccion de roles (ver README, seccion 4).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.cors(Customizer.withDefaults())
				.authorizeHttpRequests(authorize -> authorize
						// TEMPORAL: antes tenia hasRole("DESCARGAR_GUIAS") y hasRole("GESTION_GUIAS")
						// en /s3/*/object y /s3/** respectivamente. Se deja solo authenticated()
						// para destrabar pruebas mientras se configuran los App Roles en Azure.
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
		return http.build();
	}
}
