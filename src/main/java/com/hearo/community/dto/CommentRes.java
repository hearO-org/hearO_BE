package com.hearo.community.dto;

import com.hearo.community.domain.Comment;

import java.time.LocalDateTime;

public record CommentRes(
        Long id,
        Long authorId,
        String authorNickname,
        String content,
        boolean deleted,
        Long parentId,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {
    public static CommentRes of(Comment c){
        return new CommentRes(
                c.getId(),
                c.getAuthor().getId(),
                c.getAuthor().getNickname(),
                c.isDeleted() ? "(삭제된 댓글입니다)" : c.getContent(),
                c.isDeleted(),
                c.getParent() == null ? null : c.getParent().getId(),
                c.getCreatedAt(),
                c.getModifiedAt()
        );
    }
}
