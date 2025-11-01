// file: src/main/java/com/hearo/community/domain/PostLike.java
package com.hearo.community.domain;

import com.hearo.global.entity.BaseTimeEntity;
import com.hearo.user.domain.User;
import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(name = "post_likes",
       uniqueConstraints = @UniqueConstraint(name = "uq_post_like_user_post", columnNames = {"user_id","post_id"}))
public class PostLike extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id", nullable=false)
    private User user;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="post_id", nullable=false)
    private Post post;

    protected PostLike() {}
    private PostLike(User user, Post post) { this.user = user; this.post = post; }

    public static PostLike of(User user, Post post) { return new PostLike(user, post); }
}
