package cl.duoc.ejemplo.ms.administracion.archivos.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import cl.duoc.ejemplo.ms.administracion.archivos.dto.S3ObjectDto;
import cl.duoc.ejemplo.ms.administracion.archivos.exception.InvalidFileException;
import cl.duoc.ejemplo.ms.administracion.archivos.exception.S3AccessDeniedException;
import cl.duoc.ejemplo.ms.administracion.archivos.exception.S3BucketNotFoundException;
import cl.duoc.ejemplo.ms.administracion.archivos.exception.S3ObjectNotFoundException;
import cl.duoc.ejemplo.ms.administracion.archivos.exception.S3OperationException;
import cl.duoc.ejemplo.ms.administracion.archivos.exception.S3UploadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@Service
@RequiredArgsConstructor
public class AwsS3Service {

	private final S3Client s3Client;

	public List<S3ObjectDto> listObjects(String bucket) {

		try {
			log.info("Listando objetos del bucket: {}", bucket);

			ListObjectsV2Request request = ListObjectsV2Request.builder().bucket(bucket).build();

			ListObjectsV2Response response = s3Client.listObjectsV2(request);

			log.info("Se encontraron {} objetos en el bucket {}", response.contents().size(), bucket);

			return response.contents().stream()
					.map(obj -> new S3ObjectDto(obj.key(), obj.size(),
							obj.lastModified() != null ? obj.lastModified().toString() : null))
					.collect(Collectors.toList());

		} catch (NoSuchBucketException e) {
			throw new S3BucketNotFoundException(bucket, e);
		} catch (S3Exception e) {
			if (e.statusCode() == 403) {
				throw new S3AccessDeniedException("listar objetos del bucket: " + bucket, e);
			}
			throw new S3OperationException("Error al listar objetos del bucket: " + bucket, e);
		}
	}

	public byte[] downloadAsBytes(String bucket, String key) {

		try {
			log.info("Descargando objeto: {} del bucket: {}", key, bucket);

			GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(bucket).key(key).build();

			ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObjectAsBytes(getObjectRequest);

			log.info("Objeto descargado exitosamente: {}", key);

			return responseBytes.asByteArray();

		} catch (NoSuchBucketException e) {
			throw new S3BucketNotFoundException(bucket, e);
		} catch (NoSuchKeyException e) {
			throw new S3ObjectNotFoundException(key, bucket, e);
		} catch (S3Exception e) {
			if (e.statusCode() == 403) {
				throw new S3AccessDeniedException("descargar el objeto: " + key, e);
			}
			throw new S3OperationException("Error al descargar el objeto: " + key, e);
		}
	}

	/**
	 * MODIFICADO: recibe los bytes ya leidos (una sola vez, en el controller)
	 * en vez del MultipartFile original. Antes se llamaba a file.getInputStream()
	 * aqui, pero si EfsService ya habia consumido/movido el archivo temporal
	 * del multipart (via transferTo), esta segunda lectura fallaba con
	 * NoSuchFileException. Con bytes en memoria no hay una "segunda lectura"
	 * de ningun stream, asi que el orden de las operaciones ya no importa.
	 */
	public void upload(String bucket, String key, byte[] content, String contentType) {

		if (content == null || content.length == 0) {
			throw new InvalidFileException("El archivo está vacío o es nulo");
		}

		if (key == null || key.isBlank()) {
			throw new InvalidFileException("El nombre del archivo no es válido");
		}

		try {
			log.info("Subiendo archivo: {} al bucket: {}, tamaño: {} bytes", key, bucket, content.length);

			PutObjectRequest putObjectRequest = PutObjectRequest.builder().bucket(bucket).key(key)
					.contentType(contentType).contentLength((long) content.length).build();

			s3Client.putObject(putObjectRequest, RequestBody.fromBytes(content));

			log.info("Archivo subido exitosamente: {}", key);

		} catch (NoSuchBucketException e) {
			throw new S3BucketNotFoundException(bucket, e);
		} catch (S3Exception e) {
			if (e.statusCode() == 403) {
				throw new S3AccessDeniedException("subir archivo al bucket: " + bucket, e);
			}
			throw new S3UploadException("Error al subir el archivo a S3: " + e.getMessage(), e);
		}
	}

	public void moveObject(String bucket, String sourceKey, String destKey) {

		try {
			log.info("Moviendo objeto de {} a {} en el bucket: {}", sourceKey, destKey, bucket);

			CopyObjectRequest copyRequest = CopyObjectRequest.builder().sourceBucket(bucket).sourceKey(sourceKey)
					.destinationBucket(bucket).destinationKey(destKey).build();

			s3Client.copyObject(copyRequest);

			log.info("Objeto copiado exitosamente, procediendo a eliminar el origen");

			deleteObject(bucket, sourceKey);

			log.info("Objeto movido exitosamente de {} a {}", sourceKey, destKey);

		} catch (NoSuchBucketException e) {
			throw new S3BucketNotFoundException(bucket, e);
		} catch (NoSuchKeyException e) {
			throw new S3ObjectNotFoundException(sourceKey, bucket, e);
		} catch (S3Exception e) {
			if (e.statusCode() == 403) {
				throw new S3AccessDeniedException("mover objeto en el bucket: " + bucket, e);
			}
			throw new S3OperationException("Error al mover el objeto de " + sourceKey + " a " + destKey, e);
		}
	}

	public void deleteObject(String bucket, String key) {

		try {
			log.info("Eliminando objeto: {} del bucket: {}", key, bucket);

			DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder().bucket(bucket).key(key).build();

			s3Client.deleteObject(deleteRequest);

			log.info("Objeto eliminado exitosamente: {}", key);

		} catch (NoSuchBucketException e) {
			throw new S3BucketNotFoundException(bucket, e);
		} catch (S3Exception e) {
			if (e.statusCode() == 403) {
				throw new S3AccessDeniedException("eliminar objeto del bucket: " + bucket, e);
			}
			throw new S3OperationException("Error al eliminar el objeto: " + key, e);
		}
	}
}

