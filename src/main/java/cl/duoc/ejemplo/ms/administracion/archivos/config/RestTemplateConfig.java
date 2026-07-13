package cl.duoc.ejemplo.ms.administracion.archivos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

// AGREGADO: necesario para que este microservicio pueda llamar por HTTP a msrabbitmq
@Configuration
public class RestTemplateConfig {

	@Bean
	RestTemplate restTemplate() {
		return new RestTemplate();
	}
}
