package com.fanlink.domain.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.fanlink.domain.user.Team;

public interface PostRepository extends JpaRepository<Post, Long> {

	Page<Post> findByCategoryAndTeam(PostCategory category, Team team, Pageable pageable);

	Page<Post> findByCategory(PostCategory category, Pageable pageable);

	Page<Post> findByTeam(Team team, Pageable pageable);

	Page<Post> findByAuthorId(Long authorId, Pageable pageable);
}
