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
PRODUCTOR
**/
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
