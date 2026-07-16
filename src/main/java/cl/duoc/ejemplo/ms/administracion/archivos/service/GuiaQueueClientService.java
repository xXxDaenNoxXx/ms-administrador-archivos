package cl.duoc.ejemplo.ms.administracion.archivos.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import cl.duoc.ejemplo.ms.administracion.archivos.config.RabbitMQConfig;
import cl.duoc.ejemplo.ms.administracion.archivos.dto.GuiaMensajeDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuiaQueueClientService {

	private final RabbitTemplate rabbitTemplate;

	public void notificarGuiaGenerada(GuiaMensajeDTO guia) {

		try {
			guia.setEstado("GENERADA");
			rabbitTemplate.convertAndSend(RabbitMQConfig.MAIN_QUEUE, guia);
			log.info("Guia {} publicada en cola1 (myQueue)", guia.getIdGuia());
		} catch (Exception e) {
			// Si ni siquiera se pudo publicar en cola1 (ej. RabbitMQ caido),
			// se intenta el camino de error para que igual quede registro en cola2.
			log.error("No se pudo publicar la guia {} en cola1, se intenta cola2: {}", guia.getIdGuia(),
					e.getMessage());
			notificarError(guia);
		}
	}

	public void notificarError(GuiaMensajeDTO guia) {

		try {
			guia.setEstado("CON_ERROR");
			rabbitTemplate.convertAndSend(RabbitMQConfig.DLX_QUEUE, guia);
			log.info("Guia {} publicada en cola2 (errorQueue)", guia.getIdGuia());
		} catch (Exception e) {
			log.error("No se pudo publicar el error de la guia {} en cola2: {}", guia.getIdGuia(), e.getMessage());
		}
	}
}
