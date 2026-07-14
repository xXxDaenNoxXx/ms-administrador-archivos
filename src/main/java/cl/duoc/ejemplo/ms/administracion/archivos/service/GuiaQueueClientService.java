package cl.duoc.ejemplo.ms.administracion.archivos.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import cl.duoc.ejemplo.ms.administracion.archivos.dto.GuiaMensajeDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AGREGADO
 *
 * Este microservicio (ms-administracion-archivos) NO habla directamente con
 * RabbitMQ. Es msrabbitmq quien lo hace. Este cliente solo hace la llamada
 * HTTP hacia msrabbitmq para delegarle la publicacion en cola1/cola2, cumpliendo
 * con la separacion en dos servicios independientes que pide el caso.
 *
 * MODIFICADO: msrabbitmq ahora exige JWT + rol GESTION_GUIAS en /api/guias/**,
 * asi que estas llamadas deben reenviar el mismo Bearer token que trajo la
 * peticion original del usuario (no se genera un token nuevo aqui).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuiaQueueClientService {

	private final RestTemplate restTemplate;

	@Value("${msrabbitmq.base-url}")
	private String msRabbitBaseUrl;

	/**
	 * Camino feliz: la guia se subio bien a S3 -> se notifica a msrabbitmq
	 * para que la publique en cola1.
	 *
	 * @param bearerToken token completo, ej. "Bearer eyJ..." (tal cual llega
	 *                     en el header Authorization de la peticion original)
	 */
	public void notificarGuiaGenerada(GuiaMensajeDTO guia, String bearerToken) {

		try {
			restTemplate.postForEntity(msRabbitBaseUrl + "/api/guias",
					new HttpEntity<>(guia, buildHeaders(bearerToken)), String.class);
			log.info("Guia {} notificada a cola1 via msrabbitmq", guia.getIdGuia());
		} catch (Exception e) {
			// Si ni siquiera se pudo avisar a msrabbitmq (ej. el servicio esta caido,
			// o el token no tiene el rol GESTION_GUIAS), se intenta el camino de error
			// para que igual quede registro en cola2.
			log.error("No se pudo notificar la guia {} a cola1, se intenta cola2: {}", guia.getIdGuia(),
					e.getMessage());
			notificarError(guia, bearerToken);
		}
	}

	/**
	 * Camino de error: la generacion/subida de la guia fallo (ej. error de S3)
	 * -> se notifica a msrabbitmq para que la publique directo en cola2.
	 */
	public void notificarError(GuiaMensajeDTO guia, String bearerToken) {

		try {
			restTemplate.postForEntity(msRabbitBaseUrl + "/api/guias/error",
					new HttpEntity<>(guia, buildHeaders(bearerToken)), String.class);
			log.info("Guia {} notificada a cola2 (errores) via msrabbitmq", guia.getIdGuia());
		} catch (Exception e) {
			log.error("No se pudo notificar el error de la guia {} a msrabbitmq: {}", guia.getIdGuia(),
					e.getMessage());
		}
	}

	private HttpHeaders buildHeaders(String bearerToken) {
		HttpHeaders headers = new HttpHeaders();
		if (bearerToken != null && !bearerToken.isBlank()) {
			headers.set(HttpHeaders.AUTHORIZATION, bearerToken);
		}
		return headers;
	}
}

