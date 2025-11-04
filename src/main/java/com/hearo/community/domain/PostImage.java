package com.hearo.community.domain;

import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(name="post_images",
       uniqueConstraints = @UniqueConstraint(name="uq_post_image_order", columnNames={"post_id","ordering"}))
public class PostImage {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="post_id", nullable=false)
    private Post post;

    @Column(nullable=false, length=512)
    private String url;

    @Column(nullable=false)
    private int ordering;

    protected PostImage() {}

    public static PostImage of(Post post, String url, int ordering){
        PostImage i = new PostImage();
        i.post = post; i.url = url; i.ordering = ordering;
        return i;
    }
}
