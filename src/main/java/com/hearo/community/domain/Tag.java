package com.hearo.community.domain;

import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(name="tags", uniqueConstraints=@UniqueConstraint(name="uq_tag_name", columnNames="name"))
public class Tag {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=32)
    private String name;

    protected Tag() {}
    public Tag(String name){ this.name = name; }
}
