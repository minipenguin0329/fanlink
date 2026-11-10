package com.fanlink.domain.post;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import com.fanlink.common.ApiException;
import com.fanlink.domain.user.Team;
import com.fanlink.domain.user.User;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * EAGER on purpose: PostResponse always needs the author's nickname/photo, and
	 * open-in-view is off, so a LAZY proxy here throws LazyInitializationException
	 * once the controller maps the entity after the transaction/session has closed.
	 */
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "author_id", nullable = false)
	private User author;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PostCategory category;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Team team;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false, columnDefinition = "text")
	private String content;

	/** Per-unit face value in KRW. Required (and AI-verified) for TICKET posts. */
	private Integer facePrice;

	private Integer quantity;

	/** Stored capture used to OCR-verify facePrice, kept for audit/dispute review. */
	private String priceCaptureImageUrl;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PostStatus status = PostStatus.ACTIVE;

	@Column(nullable = false)
	private int viewCount = 0;

	/**
	 * EAGER for the same open-in-view reason as author. SUBSELECT avoids the
	 * classic join-fetch-collection-with-pagination Cartesian product/in-memory
	 * paging warning on GET /api/posts: it issues one extra query per page instead
	 * of joining the collection into the paginated row set.
	 */
	@OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@org.hibernate.annotations.Fetch(org.hibernate.annotations.FetchMode.SUBSELECT)
	@OrderBy("sortOrder ASC")
	private List<PostImage> images = new ArrayList<>();

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	public Post(User author, PostCategory category, Team team, String title, String content,
			Integer facePrice, Integer quantity, String priceCaptureImageUrl) {
		this.author = author;
		this.category = category;
		this.team = team;
		this.title = title;
		this.content = content;
		this.facePrice = facePrice;
		this.quantity = quantity;
		this.priceCaptureImageUrl = priceCaptureImageUrl;
		this.status = PostStatus.ACTIVE;
	}

	@PrePersist
	void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

	public void replaceImages(List<String> imageUrls) {
		this.images.clear();
		if (imageUrls == null) {
			return;
		}
		for (int i = 0; i < imageUrls.size(); i++) {
			PostImage image = new PostImage(imageUrls.get(i), i);
			image.assignTo(this);
			this.images.add(image);
		}
	}

	public void update(String title, String content, Integer facePrice, Integer quantity, List<String> imageUrls) {
		if (title != null && !title.isBlank()) {
			this.title = title;
		}
		if (content != null && !content.isBlank()) {
			this.content = content;
		}
		if (facePrice != null) {
			this.facePrice = facePrice;
		}
		if (quantity != null) {
			this.quantity = quantity;
		}
		if (imageUrls != null) {
			replaceImages(imageUrls);
		}
	}

	public void changeStatus(PostStatus status) {
		this.status = status;
	}

	public void increaseViewCount() {
		this.viewCount++;
	}

	public void verifyAuthor(Long userId) {
		if (!this.author.getId().equals(userId)) {
			throw ApiException.forbidden("본인이 작성한 게시글만 수정/삭제할 수 있습니다.");
		}
	}
}
