package com.fanlink.ocr;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Best-effort "정가" (face value) reader for OCR'd ticket-purchase-confirmation text.
 * Heuristic, not a parser: prefers a number on a line mentioning 정가/가격, otherwise
 * falls back to the smallest "N원" figure on the receipt (face price is usually smaller
 * than the total-with-fees line). Upgrade path: a layout-aware receipt parser per vendor
 * (Interpark/Ticketlink/Yes24) if false positives show up in practice.
 */
public final class TicketPriceExtractor {

	private static final Pattern WON_AMOUNT = Pattern.compile("([0-9]{1,3}(?:,[0-9]{3})+|[0-9]{4,})\\s*원");
	private static final List<String> FACE_PRICE_KEYWORDS = List.of("정가");

	private TicketPriceExtractor() {
	}

	public static OptionalInt extractFacePrice(String rawText) {
		if (rawText == null || rawText.isBlank()) {
			return OptionalInt.empty();
		}

		for (String line : rawText.split("\\R")) {
			if (FACE_PRICE_KEYWORDS.stream().anyMatch(line::contains)) {
				Optional<Integer> amount = firstAmount(line);
				if (amount.isPresent()) {
					return OptionalInt.of(amount.get());
				}
			}
		}

		List<Integer> allAmounts = new ArrayList<>();
		Matcher matcher = WON_AMOUNT.matcher(rawText);
		while (matcher.find()) {
			allAmounts.add(parse(matcher.group(1)));
		}
		return allAmounts.stream().mapToInt(Integer::intValue).min();
	}

	private static Optional<Integer> firstAmount(String line) {
		Matcher matcher = WON_AMOUNT.matcher(line);
		return matcher.find() ? Optional.of(parse(matcher.group(1))) : Optional.empty();
	}

	private static int parse(String numberWithCommas) {
		return Integer.parseInt(numberWithCommas.replace(",", ""));
	}
}
