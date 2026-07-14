package cl.duoc.ejemplo.ms.administracion.archivos.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EfsService {

	@Value("${efs.path}")
	private String efsPath;

	/**
	 * MODIFICADO: antes recibia el MultipartFile y usaba transferTo(dest), lo
	 * que en Tomcat MUEVE (no copia) el archivo temporal del multipart cuando
	 * este supera el umbral de memoria. Eso dejaba sin archivo temporal al
	 * MultipartFile original, y la siguiente lectura (AwsS3Service.upload)
	 * fallaba con NoSuchFileException. Ahora recibe los bytes ya leidos una
	 * sola vez (ver AwsS3Controller) y simplemente los escribe, sin tocar el
	 * archivo temporal de Tomcat.
	 */
	public File saveToEfs(String filename, byte[] content) throws IOException {

		File dest = new File(efsPath, filename);
		File parentDir = dest.getParentFile();
		if (parentDir != null && !parentDir.exists()) {
			parentDir.mkdirs();
		}
		Files.write(dest.toPath(), content);
		return dest;
	}
}
