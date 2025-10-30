package com.hearo.community.domain;

import com.hearo.global.entity.BaseTimeEntity;
import com.hearo.user.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Getter
@Entity
@Table(name = "posts")
public class Post extends BaseTimeEntity {

    public enum Visibility { PUBLIC, PRIVATE }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="author_id", nullable=false)
    private User author;

    @Column(nullable=false, length=120)
    private String title;

    @Lob @Column(nullable=false)
    private String content;

    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20)
    private PostCategory category = PostCategory.GENERAL;

    @Enumerated(EnumType.STRING) @Column(nullable=false, length=10)
    private Visibility visibility = Visibility.PUBLIC;

    @Setter
    @Column(nullable=false)
    private boolean deleted = false;

    // 최대 5장, 정렬 보장
    @OneToMany(mappedBy="post", cascade=CascadeType.ALL, orphanRemoval=true)
    @OrderBy("ordering ASC")
    private List<PostImage> images = new ArrayList<>();

    // 간단한 문자열 태그(정규화된 Tag 엔티티로 바꾸고 싶으면 나중에 교체)
    @ElementCollection
    @CollectionTable(name="post_tags", joinColumns=@JoinColumn(name="post_id"))
    @Column(name="tag", length=32, nullable=false)
    private Set<String> tags = new LinkedHashSet<>();

    protected Post() {}

    public static Post create(User author, String title, String content, PostCategory category, Visibility v) {
        Post p = new Post();
        p.author = author;
        p.title = title;
        p.content = content;
        p.category = category == null ? PostCategory.GENERAL : category;
        p.visibility = v == null ? Visibility.PUBLIC : v;
        return p;
    }

    public void edit(String title, String content, PostCategory category, Visibility v) {
        this.title = title;
        this.content = content;
        this.category = category == null ? this.category : category;
        this.visibility = v == null ? this.visibility : v;
    }
}
