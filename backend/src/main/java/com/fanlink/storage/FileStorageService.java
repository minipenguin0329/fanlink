package com.fanlink.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.fanlink.common.ApiException;

import jakarta.annotation.PostConstruct;

@Service
public class FileStorageService {

	@Value("${app.storage.upload-dir}")
	private String uploadDir;

	private Path root;

	@PostConstruct
	void init() {
		this.root = Path.of(uploadDir).toAbsolutePath().normalize();
		try {
			Files.createDirectories(root);
		} catch (IOException e) {
			throw new IllegalStateException("업로드 디렉터리를 생성할 수 없습니다: " + root, e);
		}
	}

	/** Stores an uploaded image and returns its public URL path, e.g. "/uploads/<uuid>.jpg". */
	public String storeImage(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw ApiException.badRequest("이미지 파일이 비어 있습니다.");
		}
		String contentType = file.getContentType();
		if (contentType == null || !contentType.startsWith("image/")) {
			throw ApiException.badRequest("이미지 파일만 업로드할 수 있습니다.");
		}

		String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
		String filename = UUID.randomUUID() + (extension != null ? "." + extension : "");
		Path target = root.resolve(filename).normalize();

		if (!target.getParent().equals(root)) {
			throw ApiException.badRequest("잘못된 파일 경로입니다.");
		}

		try {
			file.transferTo(target);
		} catch (IOException e) {
			throw new IllegalStateException("이미지 저장에 실패했습니다.", e);
		}

		return "/uploads/" + filename;
	}

	/** Reads back a previously stored image (by its "/uploads/xxx" public URL path) for server-side re-processing. */
	public byte[] readImage(String publicUrlPath) {
		String filename = publicUrlPath.substring(publicUrlPath.lastIndexOf('/') + 1);
		Path target = root.resolve(filename).normalize();
		if (!target.getParent().equals(root)) {
			throw ApiException.badRequest("잘못된 파일 경로입니다.");
		}
		try {
			return Files.readAllBytes(target);
		} catch (IOException e) {
			throw ApiException.notFound("이미지를 찾을 수 없습니다: " + publicUrlPath);
		}
	}
}
