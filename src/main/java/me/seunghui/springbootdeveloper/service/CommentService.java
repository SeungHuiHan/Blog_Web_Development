package me.seunghui.springbootdeveloper.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import me.seunghui.springbootdeveloper.Repository.ArticleRepository;
import me.seunghui.springbootdeveloper.Repository.CommentRepository;
import me.seunghui.springbootdeveloper.domain.Article;
import me.seunghui.springbootdeveloper.domain.Comment;
import me.seunghui.springbootdeveloper.dto.AddCommentRequest;
import me.seunghui.springbootdeveloper.dto.CommentResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class CommentService {
    private final CommentRepository commentRepository;
    private final ArticleRepository articleRepository;
    //1. 게시글에 맞는 한개 댓글 생성
    public Comment saveComment(AddCommentRequest request,Long articleId,String userName) {
        Article article = articleRepository.findById(articleId).orElseThrow(()->new IllegalArgumentException("Article not found"));

        // parentCommentId가 있는 경우 부모 댓글을 조회
        Comment parentComment=null;
        if(request.getParentCommentId()!=null){
            parentComment = commentRepository.findById(request.getParentCommentId()).orElseThrow(()->new IllegalArgumentException("Parent commentnot found"));

        }

        Comment savedComment = commentRepository.save(request.toEntity(userName,article,parentComment));

        return savedComment;
    }

    //2. 게시글에 달린 댓글 목록 조회
    public List<CommentResponse> getComments(Long articleId) {
       // log.info("Fetching comments for articleId: {}", articleId);
        List<Comment> comments = commentRepository.findByArticleId(articleId);
        //log.info("Found {} comments for articleId {}", comments.size(), articleId);
        return comments.stream()
                .map(CommentResponse::new)
                .collect(Collectors.toList());
    }

    //2. 게시글에 맞는 한개 댓글과 대댓글 조회
    public List<CommentResponse> getReComments(Long articleId,Long commentId) {
        // log.info("Fetching comments for articleId: {}", articleId);
        List<Comment> comments = commentRepository.findParentAndChildCommentsByArticleId(articleId,commentId);
        //log.info("Found {} comments for articleId {}", comments.size(), articleId);
        return comments.stream()
                .map(CommentResponse::new)
                .collect(Collectors.toList());
    }
    //4. 댓글 수정
    @Transactional
    public CommentResponse updateComment(Long commentId, AddCommentRequest request) {
        Comment updatedComment=commentRepository.findById(commentId).orElseThrow(()->new IllegalArgumentException("Comment not found"));
        authorizeCommentAuthor(updatedComment);
        updatedComment.update(request.getCommentContent());

        return new CommentResponse(updatedComment);
    }



    //5. 댓글 삭제
    public void deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                        .orElseThrow(()->new IllegalArgumentException("Comment not found"));
        authorizeCommentAuthor(comment);
        commentRepository.delete(comment);
    }


    // 게시글의 작성자를 확인하여 권한 검증
    private void authorizeCommentAuthor(Comment comment) {
        String userName = SecurityContextHolder.getContext().getAuthentication().getName(); // 현재 로그인된 사용자 확인
        if (!comment.getCommentAuthor().equals(userName)) { // 작성자가 아니면 예외 발생
            throw new IllegalArgumentException("not authorized");
        }
    }

}
