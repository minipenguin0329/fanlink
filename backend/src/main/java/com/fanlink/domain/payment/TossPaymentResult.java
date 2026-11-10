package com.fanlink.domain.payment;

/** Verified provider response for a completed payment. */
public record TossPaymentResult(boolean useEscrow, String escrowKey) {
}
