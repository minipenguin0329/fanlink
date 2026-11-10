package com.fanlink.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.fanlink.domain.user.Role;
import com.fanlink.domain.user.User;

public class UserPrincipal implements OAuth2User {

	private final Long userId;
	private final Role role;
	private Map<String, Object> attributes = Map.of();

	public UserPrincipal(Long userId, Role role) {
		this.userId = userId;
		this.role = role;
	}

	public static UserPrincipal of(User user) {
		return new UserPrincipal(user.getId(), user.getRole());
	}

	public UserPrincipal withAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
		return this;
	}

	public Long getUserId() {
		return userId;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}

	@Override
	public String getName() {
		return String.valueOf(userId);
	}
}
