package com.fanlink.domain.user;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = { "provider", "provider_id" }))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AuthProvider provider;

	@Column(name = "provider_id", nullable = false)
	private String providerId;

	@Column(nullable = false)
	private String nickname;

	private String email;

	private String profileImageUrl;

	@Enumerated(EnumType.STRING)
	private Team favoriteTeam;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Role role = Role.USER;

	@Column(nullable = false)
	private int level = 1;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public User(AuthProvider provider, String providerId, String nickname, String email, String profileImageUrl) {
		this.provider = provider;
		this.providerId = providerId;
		this.nickname = nickname;
		this.email = email;
		this.profileImageUrl = profileImageUrl;
		this.role = Role.USER;
		this.level = 1;
	}

	@jakarta.persistence.PrePersist
	void onCreate() {
		this.createdAt = LocalDateTime.now();
	}

	public void updateProfile(String nickname, Team favoriteTeam, String profileImageUrl) {
		if (nickname != null && !nickname.isBlank()) {
			this.nickname = nickname;
		}
		if (favoriteTeam != null) {
			this.favoriteTeam = favoriteTeam;
		}
		if (profileImageUrl != null && !profileImageUrl.isBlank()) {
			this.profileImageUrl = profileImageUrl;
		}
	}
}
