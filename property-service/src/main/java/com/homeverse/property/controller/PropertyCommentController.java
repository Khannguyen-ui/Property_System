package com.homeverse.property.controller;

import com.homeverse.property.dto.request.CommentRequest;
import com.homeverse.property.dto.response.CommentResponse;
import com.homeverse.property.service.PropertyCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/properties/comments")
@RequiredArgsConstructor
public class PropertyCommentController {

    private final PropertyCommentService commentService;

    @PostMapping
    public CommentResponse createComment(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            @RequestBody CommentRequest request
    ) {
        return commentService.createComment(userId, guestId, request);
    }

    @GetMapping("/{propertyId}")
    public Page<CommentResponse> getComments(
            @PathVariable Long propertyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return commentService.getComments(propertyId, page, size);
    }

    @GetMapping("/replies/{parentId}")
    public Page<CommentResponse> getReplies(
            @PathVariable Long parentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return commentService.getReplies(parentId, page, size);
    }

    @DeleteMapping("/{commentId}")
    public void deleteComment(
            @PathVariable Long commentId,
            @RequestHeader("X-User-Id") Long userId
    ) {
        commentService.deleteComment(commentId, userId);
    }

    @GetMapping("/count/{propertyId}")
    public long countComments(@PathVariable Long propertyId) {
        return commentService.countComments(propertyId);
    }
}