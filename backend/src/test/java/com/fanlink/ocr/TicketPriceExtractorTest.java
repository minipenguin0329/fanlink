package com.fanlink.ocr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

class TicketPriceExtractorTest {

	@Test
	void prefersTheLineThatMentionsFacePrice() {
		String receipt = """
				삼성 라이온즈 vs 키움 히어로즈
				좌석: 3루 내야지정석 13블록 8열 12,13번
				정가 26,000원
				예매수수료 500원
				""";

		OptionalInt result = TicketPriceExtractor.extractFacePrice(receipt);

		assertTrue(result.isPresent());
		assertEquals(26000, result.getAsInt());
	}

	@Test
	void fallsBackToTheSmallestWonAmountWhenNoFacePriceLineExists() {
		String receipt = "결제금액 52,000원\n좌석 2매\n수수료 1,000원";

		OptionalInt result = TicketPriceExtractor.extractFacePrice(receipt);

		assertTrue(result.isPresent());
		assertEquals(1000, result.getAsInt());
	}

	@Test
	void returnsEmptyWhenTextHasNoAmount() {
		OptionalInt result = TicketPriceExtractor.extractFacePrice("좌석 배정 중입니다");

		assertTrue(result.isEmpty());
	}
}
