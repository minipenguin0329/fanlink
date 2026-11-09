package com.fanlink.domain.user;

public enum Team {
	SAMSUNG_LIONS("삼성 라이온즈"),
	DAEGU_FC("대구FC"),
	POHANG_STEELERS("포항 스틸러스");

	private final String displayName;

	Team(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
