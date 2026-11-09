package com.fanlink.ocr;

import java.util.OptionalInt;

import org.springframework.stereotype.Service;

import com.fanlink.ocr.dto.VerifyTicketPriceResponse;

import lombok.RequiredArgsConstructor;

/**
 * Gate for "[정가양도]" ticket posts: the declared per-ticket price must not exceed
 * the face value read off the buyer's purchase-confirmation screenshot.
 */
@Service
@RequiredArgsConstructor
public class TicketPriceVerificationService {

	private final OcrClient ocrClient;

	public VerifyTicketPriceResponse verify(byte[] imageBytes, String filename, int declaredUnitPrice) {
		OcrResult ocrResult = ocrClient.extractText(imageBytes, filename);

		if (!ocrResult.configured()) {
			return new VerifyTicketPriceResponse(true, false, null, declaredUnitPrice,
					"OCR 연동이 설정되지 않아 가격을 자동 검증하지 못했습니다. 관리자 확인이 필요합니다.");
		}

		OptionalInt facePrice = TicketPriceExtractor.extractFacePrice(ocrResult.rawText());
		if (facePrice.isEmpty()) {
			return new VerifyTicketPriceResponse(true, false, null, declaredUnitPrice,
					"캡처본에서 가격 정보를 읽지 못했습니다. 관리자 확인이 필요합니다.");
		}

		int extracted = facePrice.getAsInt();
		if (declaredUnitPrice > extracted) {
			return new VerifyTicketPriceResponse(false, true, extracted, declaredUnitPrice,
					"입력한 금액(%,d원)이 정가(%,d원)를 초과했습니다. 정가 이하로만 양도할 수 있습니다.".formatted(declaredUnitPrice, extracted));
		}

		return new VerifyTicketPriceResponse(true, true, extracted, declaredUnitPrice, "정가 이하로 확인되었습니다.");
	}
}
