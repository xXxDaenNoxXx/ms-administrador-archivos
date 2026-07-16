package cl.duoc.ejemplo.ms.administracion.archivos.config;

import java.util.Map;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cl.duoc.ejemplo.ms.administracion.archivos.dto.GuiaMensajeDTO;

/**
 * AGREGADO
 *
 * MODIFICADO: ms-administracion-archivos pasa a ser el productor AMQP real
 * (antes le pedia a msrabbitmq por HTTP que publicara). La rubrica pide
 * explicitamente "un micro productor y otro micro consumidor" hablando por
 * RabbitMQ, no uno hablando HTTP y el otro AMQP.
 *
 * No se declaran Queue/Exchange/Binding aqui a proposito: la topologia
 * (colas, exchange, dead-letter) sigue siendo responsabilidad exclusiva de
 * msrabbitmq (el consumidor), que la declara en su propio RabbitMQConfig.
 * Este productor solo necesita el nombre de la cola para publicar via el
 * exchange por defecto ("") de RabbitMQ, donde el routing key = nombre de
 * la cola.
 *
 * IMPORTANTE - Jackson2JsonMessageConverter con idClassMapping:
 * msrabbitmq NO conoce la clase GuiaMensajeDTO (paquete distinto), asi que en
 * vez de dejar que el converter mande el nombre completo de la clase Java en
 * el header __TypeId__ (lo que rompe la deserializacion del lado consumidor),
 * se define un alias comun "guiaDespacho". El RabbitMQConfig de msrabbitmq
 * debe mapear ese mismo alias hacia su propio GuiaDespachoDTO.
 */
@Configuration
public class RabbitMQConfig {

	public static final String MAIN_QUEUE = "myQueue";
	public static final String DLX_QUEUE = "errorQueue";

	@Value("${spring.rabbitmq.host}")
	private String host;

	@Value("${spring.rabbitmq.port}")
	private int port;

	@Value("${spring.rabbitmq.username}")
	private String username;

	@Value("${spring.rabbitmq.password}")
	private String password;

	@Bean
	CachingConnectionFactory connectionFactory() {

		CachingConnectionFactory factory = new CachingConnectionFactory();
		factory.setHost(host);
		factory.setPort(port);
		factory.setUsername(username);
		factory.setPassword(password);
		return factory;
	}

	@Bean
	Jackson2JsonMessageConverter messageConverter() {

		Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();

		DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
		typeMapper.setTrustedPackages("*");
		typeMapper.setIdClassMapping(Map.of("guiaDespacho", GuiaMensajeDTO.class));
		converter.setJavaTypeMapper(typeMapper);

		return converter;
	}
}
