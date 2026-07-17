package cl.duoc.ejemplo.ms.administracion.archivos.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
