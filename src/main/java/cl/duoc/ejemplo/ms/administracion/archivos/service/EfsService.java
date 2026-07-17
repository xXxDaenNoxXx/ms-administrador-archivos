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
