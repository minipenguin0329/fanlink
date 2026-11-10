package com.fanlink.domain.post;

public enum PostCategory {
	TICKET("티켓"), GOODS("굿즈"), CARPOOL("카풀"), MATE("직관메이트");

	private final String displayName;

	PostCategory(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
