package com.hearo.community.domain;

import com.hearo.global.entity.BaseTimeEntity;
import com.hearo.user.domain.User;
import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(name = "comment_likes",
       uniqueConstraints = @UniqueConstraint(name = "uq_comment_like_user_comment", columnNames = {"user_id","comment_id"}))
public class CommentLike extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id", nullable=false)
    private User user;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="comment_id", nullable=false)
    private Comment comment;

    protected CommentLike() {}
    private CommentLike(User user, Comment comment) { this.user = user; this.comment = comment; }

    public static CommentLike of(User user, Comment comment) { return new CommentLike(user, comment); }
}
