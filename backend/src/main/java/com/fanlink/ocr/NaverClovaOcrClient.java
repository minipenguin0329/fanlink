package com.fanlink.ocr;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Naver CLOVA OCR (General) client. Needs NAVER_OCR_INVOKE_URL / NAVER_OCR_SECRET_KEY
 * from the console at https://www.ncloud.com (OCR -> General) — see README.
 */
@Component
public class NaverClovaOcrClient implements OcrClient {

	private final RestClient restClient = RestClient.create();

	@Value("${app.ocr.naver.invoke-url}")
	private String invokeUrl;

	@Value("${app.ocr.naver.secret-key}")
	private String secretKey;

	@Override
	public OcrResult extractText(byte[] imageBytes, String filename) {
		if (!StringUtils.hasText(invokeUrl) || !StringUtils.hasText(secretKey)) {
			return new OcrResult("", false);
		}

		String format = extensionOf(filename);
		Map<String, Object> requestBody = Map.of(
				"version", "V2",
				"requestId", UUID.randomUUID().toString(),
				"timestamp", System.currentTimeMillis(),
				"images", List.of(Map.of(
						"format", format,
						"name", "fanlink-ticket",
						"data", Base64.getEncoder().encodeToString(imageBytes))));

		NaverOcrResponse response = restClient.post()
				.uri(invokeUrl)
				.header("X-OCR-SECRET", secretKey)
				.contentType(org.springframework.http.MediaType.APPLICATION_JSON)
				.body(requestBody)
				.retrieve()
				.body(NaverOcrResponse.class);

		String rawText = response == null ? "" : response.images().stream()
				.flatMap(image -> image.fields().stream())
				.map(NaverOcrResponse.Field::inferText)
				.collect(Collectors.joining("\n"));

		return new OcrResult(rawText, true);
	}

	private String extensionOf(String filename) {
		String ext = StringUtils.getFilenameExtension(filename);
		return (ext == null || ext.isBlank()) ? "jpg" : ext.toLowerCase();
	}

	private record NaverOcrResponse(List<Image> images) {
		private record Image(List<Field> fields) {
		}

		private record Field(String inferText) {
		}
	}
}
