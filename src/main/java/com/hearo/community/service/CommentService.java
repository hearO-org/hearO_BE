package com.hearo.community.service;

import com.hearo.community.domain.Comment;
import com.hearo.community.domain.Post;
import com.hearo.community.dto.CommentCreateReq;
import com.hearo.community.dto.CommentEditReq;
import com.hearo.community.dto.CommentRes;
import com.hearo.community.repository.CommentRepository;
import com.hearo.community.repository.PostRepository;
import com.hearo.user.domain.User;
import com.hearo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private final CommentRepository comments;
    private final PostRepository posts;
    private final UserRepository users;

    @Transactional(readOnly = true)
    public Page<CommentRes> listComments(Long postId, int page, int size) {
        Post post = posts.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시물을 찾을 수 없습니다."));
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        return comments.findByPostAndParentIsNullAndDeletedFalse(post, pageable).map(CommentRes::of);
    }

    @Transactional(readOnly = true)
    public Page<CommentRes> listReplies(Long parentCommentId, int page, int size) {
        Comment parent = comments.findById(parentCommentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        return comments.findByParentAndDeletedFalse(parent, pageable).map(CommentRes::of);
    }

    /** 내가 작성한 댓글 목록 */
    @Transactional(readOnly = true)
    public Page<CommentRes> listMyComments(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return comments.findByAuthor_IdAndDeletedFalse(userId, pageable)
                .map(CommentRes::of);
    }

    public Long write(Long userId, Long postId, CommentCreateReq req) {
        Post post = posts.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시물을 찾을 수 없습니다."));
        User author = users.findById(userId).orElseThrow();
        Comment c = Comment.of(post, author, req.content(), null);
        comments.save(c);
        return c.getId();
    }

    public Long reply(Long userId, Long postId, Long parentCommentId, CommentCreateReq req) {
        Post post = posts.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시물을 찾을 수 없습니다."));
        Comment parent = comments.findById(parentCommentId)
                .orElseThrow(() -> new IllegalArgumentException("부모 댓글을 찾을 수 없습니다."));
        if (!parent.getPost().getId().equals(post.getId()))
            throw new IllegalArgumentException("부모 댓글과 게시물이 일치하지 않습니다.");

        User author = users.findById(userId).orElseThrow();
        Comment c = Comment.of(post, author, req.content(), parent);
        comments.save(c);
        return c.getId();
    }

    public void edit(Long userId, Long commentId, CommentEditReq req) {
        Comment c = comments.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
        if (!c.getAuthor().getId().equals(userId))
            throw new SecurityException("수정 권한이 없습니다.");
        c.edit(req.content());
    }

    public void delete(Long userId, Long commentId) {
        // 활성 댓글만 삭제 가능(게시물/댓글 둘 중 하나라도 삭제면 예외)
        var c = comments.findActiveById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
        if (!c.getAuthor().getId().equals(userId))
            throw new SecurityException("삭제 권한이 없습니다.");

        if (c.isReply()) {
            // 대댓글이면 자기 자신만 soft delete
            c.softDelete();
        } else {
            // 부모 댓글이면: 자신의 모든 대댓글 soft delete 후, 부모도 soft delete
            comments.softDeleteAllByParentId(c.getId());
            c.softDelete();
        }
    }
}
