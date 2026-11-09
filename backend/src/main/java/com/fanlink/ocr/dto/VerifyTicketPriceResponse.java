package com.fanlink.ocr.dto;

public record VerifyTicketPriceResponse(
		boolean valid,
		boolean verified,
		Integer extractedFacePrice,
		int declaredUnitPrice,
		String message) {
}
