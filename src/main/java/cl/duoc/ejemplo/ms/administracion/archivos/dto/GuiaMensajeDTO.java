package cl.duoc.ejemplo.ms.administracion.archivos.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AGREGADO
 *
 * Payload que se envia por HTTP hacia msrabbitmq (POST /api/guias o
 * /api/guias/error) cada vez que se sube/genera una guia de despacho.
 * Sus campos deben coincidir con los de GuiaDespachoDTO en msrabbitmq,
 * ya que ambos microservicios son independientes y solo comparten el
 * contrato JSON, no código.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GuiaMensajeDTO {

	private String idGuia;
	private String transportista;
	private LocalDate fechaDespacho;
	private String bucket;
	private String key;
	private Long tamanioBytes;
	private String estado;
}
