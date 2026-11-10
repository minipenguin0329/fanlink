package com.fanlink.ocr;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fanlink.common.ApiException;
import com.fanlink.ocr.dto.VerifyTicketPriceResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
public class PriceVerificationController {

	private final TicketPriceVerificationService verificationService;

	/** Called live from the "글 작성" screen to grey out the submit button before the post even exists. */
	@PostMapping("/verify-ticket-price")
	public VerifyTicketPriceResponse verifyTicketPrice(
			@RequestParam("file") MultipartFile file,
			@RequestParam("declaredUnitPrice") int declaredUnitPrice) {
		try {
			return verificationService.verify(file.getBytes(), file.getOriginalFilename(), declaredUnitPrice);
		} catch (java.io.IOException e) {
			throw ApiException.badRequest("이미지를 읽을 수 없습니다.");
		}
	}
}
