package com.fanlink.storage;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

	private final FileStorageService fileStorageService;

	@PostMapping
	public Map<String, String> upload(@RequestParam("file") MultipartFile file) {
		return Map.of("url", fileStorageService.storeImage(file));
	}
}
