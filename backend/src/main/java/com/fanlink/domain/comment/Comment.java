package com.fanlink.domain.comment;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import com.fanlink.common.ApiException;
import com.fanlink.domain.post.Post;
import com.fanlink.domain.user.User;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "post_id", nullable = false)
	private Post post;

	/** EAGER: CommentResponse always needs the author's nickname/photo (see Post.author for why). */
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "author_id", nullable = false)
	private User author;

	/** Null for a top-level comment; one level of reply nesting, matching the "답글쓰기" UI. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_id")
	private Comment parent;

	@Column(nullable = false)
	private String content;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public Comment(Post post, User author, Comment parent, String content) {
		this.post = post;
		this.author = author;
		this.parent = parent;
		this.content = content;
	}

	@PrePersist
	void onCreate() {
		this.createdAt = LocalDateTime.now();
	}

	public void verifyAuthor(Long userId) {
		if (!this.author.getId().equals(userId)) {
			throw ApiException.forbidden("본인이 작성한 댓글만 삭제할 수 있습니다.");
		}
	}
}
