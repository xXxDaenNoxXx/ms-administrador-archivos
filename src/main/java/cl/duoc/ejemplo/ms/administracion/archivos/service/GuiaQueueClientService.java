package cl.duoc.ejemplo.ms.administracion.archivos.service;

import org.springframework.beans.factory.annotation.Value;
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
	 */
	public void notificarGuiaGenerada(GuiaMensajeDTO guia) {

		try {
			restTemplate.postForEntity(msRabbitBaseUrl + "/api/guias", guia, String.class);
			log.info("Guia {} notificada a cola1 via msrabbitmq", guia.getIdGuia());
		} catch (Exception e) {
			// Si ni siquiera se pudo avisar a msrabbitmq (ej. el servicio esta caido),
			// se intenta el camino de error para que igual quede registro en cola2.
			log.error("No se pudo notificar la guia {} a cola1, se intenta cola2: {}", guia.getIdGuia(),
					e.getMessage());
			notificarError(guia);
		}
	}

	/**
	 * Camino de error: la generacion/subida de la guia fallo (ej. error de S3)
	 * -> se notifica a msrabbitmq para que la publique directo en cola2.
	 */
	public void notificarError(GuiaMensajeDTO guia) {

		try {
			restTemplate.postForEntity(msRabbitBaseUrl + "/api/guias/error", guia, String.class);
			log.info("Guia {} notificada a cola2 (errores) via msrabbitmq", guia.getIdGuia());
		} catch (Exception e) {
			log.error("No se pudo notificar el error de la guia {} a msrabbitmq: {}", guia.getIdGuia(),
					e.getMessage());
		}
	}
}
