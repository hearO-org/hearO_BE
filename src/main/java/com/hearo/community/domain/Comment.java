package com.hearo.community.domain;

import com.hearo.global.entity.BaseTimeEntity;
import com.hearo.user.domain.User;
import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(name="comments", indexes = {
        @Index(name="idx_comment_post_created", columnList="post_id, created_at")
})
public class Comment extends BaseTimeEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="post_id", nullable=false)
    private Post post;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="author_id", nullable=false)
    private User author;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="parent_id")
    private Comment parent; // null이면 댓글, 있으면 대댓글

    @Column(nullable=false, length=2000)
    private String content;

    @Column(nullable=false)
    private boolean deleted = false;

    protected Comment() {}

    public static Comment of(Post post, User author, String content, Comment parent){
        Comment c = new Comment();
        c.post = post; c.author = author; c.content = content; c.parent = parent;
        return c;
    }

    public boolean isReply(){ return parent != null; }

    public void edit(String content){ this.content = content; }

    public void softDelete(){ this.deleted = true; }
}
