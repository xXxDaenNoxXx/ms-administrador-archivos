package cl.duoc.ejemplo.ms.administracion.archivos.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cl.duoc.ejemplo.ms.administracion.archivos.dto.GuiaMensajeDTO;
import cl.duoc.ejemplo.ms.administracion.archivos.dto.S3ObjectDto;
import cl.duoc.ejemplo.ms.administracion.archivos.service.AwsS3Service;
import cl.duoc.ejemplo.ms.administracion.archivos.service.EfsService;
import cl.duoc.ejemplo.ms.administracion.archivos.service.GuiaQueueClientService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/s3")
@RequiredArgsConstructor
public class AwsS3Controller {

	@Autowired
	private AwsS3Service awsS3Service;

	@Autowired
	private EfsService efsService;

	// AGREGADO
	@Autowired
	private GuiaQueueClientService guiaQueueClientService;

	/**
	 * Lista todos los objetos en un bucket de S3
	 */
	@GetMapping("/{bucket}/objects")
	public ResponseEntity<List<S3ObjectDto>> listObjects(@PathVariable String bucket) {
		List<S3ObjectDto> dtoList = awsS3Service.listObjects(bucket);
		return ResponseEntity.ok(dtoList);
	}

	/**
	 * Descarga un objeto de S3 como array de bytes
	 */
	@GetMapping("/{bucket}/object")
	public ResponseEntity<byte[]> downloadObject(@PathVariable String bucket, @RequestParam String key) {
		byte[] fileBytes = awsS3Service.downloadAsBytes(bucket, key);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + key + "\"")
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.body(fileBytes);
	}

	/**
	 * Sube un archivo a S3 via multipart/form-data
	 */
	@PostMapping(value = "/{bucket}/object", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Void> uploadObject(@PathVariable String bucket, @RequestParam String key,
			@RequestParam("file") MultipartFile file) throws IOException {

		// MODIFICADO: se arma el mensaje de la guia antes de intentar la subida,
		// para poder notificar tanto el exito como el error hacia msrabbitmq.
		GuiaMensajeDTO guia = new GuiaMensajeDTO(UUID.randomUUID().toString(), "N/D", LocalDate.now(), bucket, key,
				file.getSize(), null);
		try {

			byte[] fileBytes = file.getBytes();

			efsService.saveToEfs(key, fileBytes);
			awsS3Service.upload(bucket, key, fileBytes, file.getContentType());

			// se envia a cola1
			guiaQueueClientService.notificarGuiaGenerada(guia);

			return ResponseEntity.status(HttpStatus.CREATED).build();
		} catch (Exception e) {
			e.printStackTrace();

			// se envia directo a cola2 (errores)
			guiaQueueClientService.notificarError(guia);

			return ResponseEntity.internalServerError().build();
		}
	}

	/**
	 * Sube un archivo a S3 via application/octet-stream (compatible con API Gateway)
	 */
	@PostMapping(value = "/{bucket}/object", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public ResponseEntity<Void> uploadObjectBinary(@PathVariable String bucket, @RequestParam String key,
			HttpServletRequest request) {
		try {
			byte[] bytes = request.getInputStream().readAllBytes();

			efsService.saveToEfs(key, bytes);
			awsS3Service.upload(bucket, key, bytes, "application/octet-stream");

			//se envia a cola1
			GuiaMensajeDTO guia = new GuiaMensajeDTO(UUID.randomUUID().toString(), "N/D", LocalDate.now(), bucket,
					key, (long) bytes.length, null);
			guiaQueueClientService.notificarGuiaGenerada(guia);

			return ResponseEntity.status(HttpStatus.CREATED).build();
		} catch (Exception e) {
			e.printStackTrace();

			// se envia directo a cola2 (errores)
			GuiaMensajeDTO guiaError = new GuiaMensajeDTO(UUID.randomUUID().toString(), "N/D", LocalDate.now(),
					bucket, key, null, null);
			guiaQueueClientService.notificarError(guiaError);

			return ResponseEntity.internalServerError().build();
		}
	}

	/**
	 * Mueve un objeto dentro del mismo bucket
	 */
	@PostMapping("/{bucket}/move")
	public ResponseEntity<Void> moveObject(@PathVariable String bucket, @RequestParam String sourceKey,
			@RequestParam String destKey) {
		awsS3Service.moveObject(bucket, sourceKey, destKey);
		return ResponseEntity.ok().build();
	}

	/**
	 * Elimina un objeto de S3
	 */
	@DeleteMapping("/{bucket}/object")
	public ResponseEntity<Void> deleteObject(@PathVariable String bucket, @RequestParam String key) {
		awsS3Service.deleteObject(bucket, key);
		return ResponseEntity.noContent().build();
	}
}
