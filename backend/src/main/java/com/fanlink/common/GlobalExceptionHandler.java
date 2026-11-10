package com.fanlink.common;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<Map<String, Object>> handleApiException(ApiException ex) {
		return ResponseEntity.status(ex.getStatus()).body(errorBody(ex.getStatus(), ex.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(e -> e.getField() + ": " + e.getDefaultMessage())
				.orElse("유효성 검증에 실패했습니다.");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(HttpStatus.BAD_REQUEST, message));
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<Map<String, Object>> handleUploadSize(MaxUploadSizeExceededException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(errorBody(HttpStatus.BAD_REQUEST, "파일 크기는 10MB를 초과할 수 없습니다."));
	}

	private Map<String, Object> errorBody(HttpStatus status, String message) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("status", status.value());
		body.put("message", message);
		return body;
	}
}
